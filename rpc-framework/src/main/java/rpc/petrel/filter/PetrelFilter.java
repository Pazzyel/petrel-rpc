package rpc.petrel.filter;

import rpc.petrel.extension.SPI;

@SPI
public interface PetrelFilter {
    void invoke(Invocation invocation);

}
