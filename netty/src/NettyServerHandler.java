/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.util.CharsetUtil;
import java.util.Random;
import com.lambdaworks.redis.*;

public class NettyServerHandler extends SimpleChannelUpstreamHandler {

    private static String connWatch = "connWatch";
    private static RedisAsyncConnection<String, String> m_async;

    private HttpRequest request;
    private boolean readingChunks;



    /** Buffer that stores the response content */
    private final StringBuilder buf = new StringBuilder();

    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        // Suspend incoming traffic until connected to the remote host.
        synchronized (connWatch){
            if(m_async == null){
                final Channel inboundChannel = e.getChannel();
                inboundChannel.setReadable(false);
                RedisClient client = new RedisClient("10.174.178.235", 6379);
                m_async = client.connectAsync();
                ChannelFuture f = m_async.future;
                f.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            // Connection attempt succeeded:
                            // Begin to accept incoming traffic.
                            inboundChannel.setReadable(true);
                        } else {
                            // Close the connection if the connection attempt has failed.
                            inboundChannel.close();
                        }
                    }
                });
            }
        }
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            HttpRequest request = this.request = (HttpRequest) e.getMessage();

            if (is100ContinueExpected(request)) {
                send100Continue(e);
            }

            buf.setLength(0);
            buf.append("Netty Test\r\n");

            Future<String> get = m_async.get("user_data");

            m_async.awaitAll(get);
            String raw = get.get();
            raw = decompress(raw);
            ObjectMapper m = new ObjectMapper();
            JsonNode tree = m.readTree(raw);
            JsonNode currVal = (JsonNode)tree.path("TWIDDLE");
            //ValueNode newVal = new ValueNode();
            //buf.append("Curr Val: " + currVal.longValue());
            ///Random rn = new Random();
            //ValueNode val = new ValueNode();

            //((ObjectNode)tree.set("TWIDDLE", rn.nextInt(30000));
            for(int i = 0; i < 100; i++){
                for(int j = 0; j < 100; j++){
                    double k = Math.sin(i) * Math.sin(j);
                }
            }

            String jsonEncoded = m.writeValueAsString(tree);
            String compressed = compress(jsonEncoded);
            Future<String> set = m_async.set("user_data", compressed);
            m_async.awaitAll(set);
            //buf.append("jsonEncoded:" + jsonEncoded);
            //buf.append("compressed:" + compressed);
            buf.append("\r\n");

            writeResponse(e);

    }

    public static String compress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        String outStr = out.toString("ISO-8859-1");
        return outStr;
    }

    public static String decompress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes("ISO-8859-1")));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "ISO-8859-1"));
        String outStr = "";
        String line;
        while ((line=bf.readLine())!=null) {
            outStr += line;
        }
        return outStr;
    }

    private void writeResponse(MessageEvent e) {
        // Decide whether to close the connection or not.
        boolean keepAlive = isKeepAlive(request);

        // Build the response object.
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Encode the cookie.
        String cookieString = request.getHeader(COOKIE);
        if (cookieString != null) {
            CookieDecoder cookieDecoder = new CookieDecoder();
            Set<Cookie> cookies = cookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                CookieEncoder cookieEncoder = new CookieEncoder(true);
                for (Cookie cookie : cookies) {
                    cookieEncoder.addCookie(cookie);
                    response.addHeader(SET_COOKIE, cookieEncoder.encode());
                }
            }
        } else {
            // Browser sent no cookie.  Add some.
            CookieEncoder cookieEncoder = new CookieEncoder(true);
            cookieEncoder.addCookie("key1", "value1");
            response.addHeader(SET_COOKIE, cookieEncoder.encode());
            cookieEncoder.addCookie("key2", "value2");
            response.addHeader(SET_COOKIE, cookieEncoder.encode());
        }

        // Write the response.
        ChannelFuture future = e.getChannel().write(response);

        // Close the non-keep-alive connection after the write operation is done.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void send100Continue(MessageEvent e) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
        e.getChannel().write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
