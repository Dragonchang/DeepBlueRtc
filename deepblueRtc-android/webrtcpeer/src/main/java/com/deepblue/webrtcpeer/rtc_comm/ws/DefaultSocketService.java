package com.deepblue.webrtcpeer.rtc_comm.ws;

import android.app.Application;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by nhancao on 6/19/17.
 */

public class DefaultSocketService implements SocketService {
    private static final String TAG = DefaultSocketService.class.getSimpleName();
    private WebSocketClient client;
    private KeyStore keyStore;
    private LooperExecutor executor;

    private Application application;
    private SocketCallBack socketCallBack;

    public DefaultSocketService(Application application) {
        this.application = application;

        this.executor = new LooperExecutor();
        this.executor.requestStart();
    }

    @Override
    public void connect(String host) {
        connect(host, true);
    }

    @Override
    public void connect(String host, boolean force) {
        if (force) {
            close();
        } else if (isConnected()) {
            return;
        }

        URI uri;
        try {
            uri = new URI(host);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                if (socketCallBack != null) {
                    socketCallBack.onOpen(serverHandshake);
                }
            }

            @Override
            public void onMessage(String s) {
                if (socketCallBack != null) {
                    socketCallBack.onMessage(s);
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                if (socketCallBack != null) {
                    socketCallBack.onClose(i, s, b);
                }
            }

            @Override
            public void onError(Exception e) {
                if (socketCallBack != null) {
                    socketCallBack.onError(e);
                }

            }
        };

        try {
            String scheme = uri.getScheme();
            if (scheme.equals("https") || scheme.equals("wss")) {
                setTrustedCertificate(application.getAssets().open("server.crt"));
                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                client.setSocket(sslContext.getSocketFactory().createSocket());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.connect();
    }

    @Override
    public void connect(String host, SocketCallBack socketCallBack) {
        connect(host, socketCallBack, true);
    }

    @Override
    public void connect(String host, SocketCallBack socketCallBack, boolean force) {
        setCallBack(socketCallBack);
        connect(host, force);
    }

    @Override
    public void setCallBack(SocketCallBack socketCallBack) {
        this.socketCallBack = socketCallBack;
    }

    @Override
    public void close() {
        if (isConnected()) {
            client.close();
        }
    }

    @Override
    public boolean isConnected() {
        return client != null && client.getConnection().isOpen();
    }

    @Override
    public void sendMessage(String message) {
        executor.execute(() -> {
            if (isConnected()) {
                try {
                    Log.i(TAG, "send: " + message);
                    client.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setTrustedCertificate(InputStream inputFile) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(inputFile);
            Certificate ca = cf.generateCertificate(caInput);

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
