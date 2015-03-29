package hms.webrtc.udp.proxy;

import com.google.common.base.Optional;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ProxyBackEndHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Autowired
    @Qualifier("proxyContextCache")
    private ProxyContextCache proxyContextCache;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        Optional<ProxyContext> proxyContextOptional = proxyContextCache.get(ForwardResolver.getKeyForEndpoint(msg));
        if(proxyContextOptional.isPresent()) {
            ProxyContext proxyContext = proxyContextOptional.get();
            msg.content().retain();
            proxyContext.getInboundChannel().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(msg.content()), proxyContext.getSender()));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}