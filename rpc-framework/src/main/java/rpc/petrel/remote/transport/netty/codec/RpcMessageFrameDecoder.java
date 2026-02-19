package rpc.petrel.remote.transport.netty.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import rpc.petrel.remote.constants.RpcConstants;

public class RpcMessageFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageFrameDecoder() {
        super(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }
}
