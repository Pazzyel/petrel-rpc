package rpc.pazz.remote.transport.netty.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import rpc.pazz.remote.constants.RpcConstants;

public class RpcMessageFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageFrameDecoder() {
        super(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }
}
