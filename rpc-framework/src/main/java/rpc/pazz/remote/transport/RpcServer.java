package rpc.pazz.remote.transport;

import rpc.pazz.extension.SPI;

@SPI
public interface RpcServer {
    void start();
}
