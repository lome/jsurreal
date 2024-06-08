package org.lome.jsurreal.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.netty.ws.NettyWebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.lome.jsurreal.protocol.command.*;
import org.lome.jsurreal.protocol.exception.ConnectionClosedException;
import org.lome.jsurreal.protocol.exception.RequestLimitExceededException;
import org.lome.jsurreal.protocol.exception.RequestTimeoutException;
import org.lome.jsurreal.protocol.exception.SurrealCallException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class SurrealDBClient implements AutoCloseable {

    final static int MAX_REQUEST_ID = (int)1e9;
    final static int MAX_CONCURRENT_REQUEST = (int)1e5;
    final static ObjectMapper objectMapper = new ObjectMapper();

    final String host;
    final int port;
    final boolean secure;
    final String url;
    final AsyncHttpClient asyncHttpClient;
    final SynchronousQueue<SurrealResponse>[] waitingResponse;
    final LinkedBlockingDeque<String> messageDispatchingQueue;
    final Thread dispatchingThread;
    final ThreadFactory factory = Thread.ofVirtual().name("surrealdb-client-", 0).factory();
    final ExecutorService executor = Executors.newThreadPerTaskExecutor(factory);
    int requestIdCounter = 0;
    Optional<NettyWebSocket> webSocket = Optional.empty();
    boolean closed = false;

    public SurrealDBClient(String host, int port, boolean secure) {
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.url = buildUrl();
        this.asyncHttpClient = asyncHttpClient();
        this.waitingResponse = new SynchronousQueue[MAX_CONCURRENT_REQUEST];
        this.messageDispatchingQueue = new LinkedBlockingDeque<>();
        this.dispatchingThread = new Thread(() -> {
            while(!closed){
                try{
                    String message = messageDispatchingQueue.poll(150, TimeUnit.MILLISECONDS);
                    if (message != null){
                        SurrealResponse response = objectMapper.readValue(message, SurrealResponse.class);
                        int idx = Integer.valueOf(response.getId())%MAX_CONCURRENT_REQUEST;
                        SynchronousQueue<SurrealResponse> callback = waitingResponse[idx];
                        if (callback != null){
                            if (!callback.offer(response)){
                                //TODO: Huge error here!
                            }
                        } else {
                            //TODO: Huge error here!
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            System.out.println("Dispatching thread closed.");
        });
        this.dispatchingThread.start();
    }

    private String buildUrl() {
        return String.format("%s://%s:%s/rpc", secure ? "wss" : "ws", host, port);
    }

    public SurrealDBClient(String host, int port){
        this(host, port, false);
    }

    public boolean isOpen(){
        return webSocket
                .map(NettyWebSocket::isOpen)
                .orElse(false);
    }

    public boolean isClosed(){
        return closed || !isOpen();
    }

    public boolean isInitialized(){
        return !closed && webSocket.isPresent();
    }

    public void connect(){
        if (webSocket.isPresent()) throw new RuntimeException("Already connected");
        if (closed) throw new RuntimeException("Client has been closed");
        try {
            LinkedBlockingDeque<Object> waitOpen = new LinkedBlockingDeque<>();
            this.webSocket = Optional.of(asyncHttpClient.prepareGet(this.url)
                    .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                            new WebSocketListener(){

                                StringBuilder messageBuilder = new StringBuilder();

                                @Override
                                public void onOpen(org.asynchttpclient.ws.WebSocket webSocket) {
                                    // TODO: Log here
                                    waitOpen.offer(new Object());
                                }

                                @Override
                                public void onClose(org.asynchttpclient.ws.WebSocket webSocket, int i, String s) {
                                    // TODO: Log & Handle
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    // TODO: Log & Handle
                                }

                                @Override
                                public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
                                    // TODO: Log & Handle
                                    WebSocketListener.super.onBinaryFrame(payload, finalFragment, rsv);
                                }

                                @Override
                                public void onTextFrame(String payload, boolean finalFragment, int rsv) {
                                    messageBuilder.append(payload);
                                    if (finalFragment){
                                        messageDispatchingQueue.add(messageBuilder.toString());
                                        messageBuilder = new StringBuilder();
                                    }
                                }

                                @Override
                                public void onPingFrame(byte[] payload) {
                                    // TODO: Log & Handle
                                    WebSocketListener.super.onPingFrame(payload);
                                }

                                @Override
                                public void onPongFrame(byte[] payload) {
                                    // TODO: Log & Handle
                                    WebSocketListener.super.onPongFrame(payload);
                                }
                            }).build()).get());
            waitOpen.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Future<SurrealResponse> executeRequestAsync(String method, Object... parameters) throws RequestLimitExceededException {
        if (closed) throw new RuntimeException("Client has been closed");
        return executor.submit(new RpcRunnable(method, parameters));
    }

    private <T> SurrealResponse executeRequest(String method, Object... parameters) throws RequestLimitExceededException {
        try {
            return executeRequestAsync(method, parameters).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized int nextId(){
        return requestIdCounter++%MAX_REQUEST_ID;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.executor.shutdown();
        webSocket.ifPresent((s) -> {
            try {
                s.sendCloseFrame().get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.executor.shutdownNow();
        this.asyncHttpClient.close();
    }

    class RpcRunnable implements Callable<SurrealResponse> {
        final int id = nextId();
        final int idx = id%MAX_CONCURRENT_REQUEST;
        final SynchronousQueue<SurrealResponse> queue;
        final SurrealRequest request;

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        private int timeout = 30; //seconds

        public RpcRunnable(String method, Object[] params) throws RequestLimitExceededException {
            this.request = new SurrealRequest(String.valueOf(id), method, params);
            this.queue = new SynchronousQueue<>();
            if (waitingResponse[idx] != null) throw new RequestLimitExceededException();
            waitingResponse[idx] = queue;
        }


        @Override
        public SurrealResponse call() throws Exception {
            try {
                if (isClosed()) throw new ConnectionClosedException();
                webSocket.get().sendTextFrame(objectMapper.writeValueAsString(this.request));
                SurrealResponse response = queue.poll(timeout, TimeUnit.SECONDS);
                if (response == null) {
                    throw new RequestTimeoutException();
                } else {
                    return response;
                }
            }finally {
                waitingResponse[idx] = null;
            }
        }
    }

    private <T> T exec(Command command, Class<T> resultClass, Object... params) throws RequestLimitExceededException, SurrealCallException {
        return trapException(executeRequest(command.getSymbol(), cleanNulls(params)), resultClass);
    }

    private <T> List<T> execAndReturnList(Command command, Class<T> itemClass, Object... params) throws RequestLimitExceededException, SurrealCallException {
        return trapExceptionReturnList(executeRequest(command.getSymbol(), cleanNulls(params)), itemClass);
    }

    private <R extends BaseRequest,T> T exec(Command command, Class<T> resultClass, R request) throws RequestLimitExceededException, SurrealCallException {
        return trapException(executeRequest(command.getSymbol(), cleanNulls(request.toRequestParameters())), resultClass);
    }

    private Object[] cleanNulls(Object[] params) {
        return Arrays.asList(params)
                .stream()
                .filter(Objects::nonNull)
                .toArray();

    }

    private <T> T trapException(SurrealResponse response, Class<T> resultClass) throws SurrealCallException {
        if (response.getError() != null){
            throw new SurrealCallException(response.getError().getCode(), response.getError().getMessage());
        }
        if (resultClass.equals(Void.class)) return null;
        return response.getResultAs(resultClass);
    }

    private <T> List<T> trapExceptionReturnList(SurrealResponse response, Class<T> itemClass) throws SurrealCallException {
        if (response.getError() != null){
            throw new SurrealCallException(response.getError().getCode(), response.getError().getMessage());
        }
        return response.getResultAsList(itemClass);
    }

    /************** Raw Methods *************/
    public String signIn(SignIn request) throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.SIGNIN, String.class, request);
    }

    public void use(String namespace, String database) throws RequestLimitExceededException, SurrealCallException {
        exec(Command.USE, Void.class, namespace, database);
    }

    public InfoResponse info() throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.INFO, InfoResponse.class);
    }

    public QueryResponse query(QueryRequest request) throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.QUERY, QueryResponse.class, request);
    }

    public void let(String name, Object value) throws RequestLimitExceededException, SurrealCallException {
        exec(Command.LET, Void.class, name, value);
    }

    public void unset(String name) throws RequestLimitExceededException, SurrealCallException {
        exec(Command.UNSET, Void.class, name);
    }

    public void invalidate() throws RequestLimitExceededException, SurrealCallException {
        exec(Command.INVALIDATE, Void.class);
    }

    public void authenticate(String token) throws RequestLimitExceededException, SurrealCallException {
        exec(Command.AUTHENTICATE, Void.class, token);
    }

    public void kill(String uuid) throws RequestLimitExceededException, SurrealCallException {
        exec(Command.KILL, Void.class, uuid);
    }

    public <T> T select(String id, Class<T> targetClass) throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.SELECT, targetClass, id);
    }

    public <T> List<T> selectTable(String table, Class<T> targetClass) throws RequestLimitExceededException, SurrealCallException {
        return execAndReturnList(Command.SELECT, targetClass, table);
    }

    public <T> List<T> create(String target, T data) throws RequestLimitExceededException, SurrealCallException {
        return execAndReturnList(Command.CREATE, (Class<T>)data.getClass(), target, data);
    }

    public <T> List<T> insert(String table, List<T> data) throws RequestLimitExceededException, SurrealCallException {
        if (data.isEmpty()) return List.of();
        return execAndReturnList(Command.INSERT, (Class<T>)data.getFirst().getClass(), table, data);
    }

    public <T> T update(String id, T data) throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.UPDATE, (Class<T>)data.getClass(), id, data);
    }

    public <T> List<T> updateTable(String table, T data) throws RequestLimitExceededException, SurrealCallException {
        return execAndReturnList(Command.UPDATE, (Class<T>)data.getClass(), table, data);
    }

    public <T> T merge(String id, Class<T> targetClass, Object mergeData) throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.MERGE, targetClass, id, mergeData);
    }

    public <T> List<T> mergeTable(String table, Class<T> targetClass, Object mergeData) throws RequestLimitExceededException, SurrealCallException {
        return execAndReturnList(Command.MERGE, targetClass, table, mergeData);
    }

    //TODO: describe patch data
    public List<Object> patch(String target, Object patchData) throws RequestLimitExceededException, SurrealCallException {
        return execAndReturnList(Command.PATCH, Object.class, target, patchData);
    }

    public <T> T delete(String id, Class<T> targetClass) throws RequestLimitExceededException, SurrealCallException {
        return exec(Command.DELETE, targetClass, id);
    }

    public <T> List<T> deleteTable(String table, Class<T> targetClass) throws RequestLimitExceededException, SurrealCallException {
        return execAndReturnList(Command.DELETE, targetClass, table);
    }



}
