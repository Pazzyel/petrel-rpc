package rpc.petrel.exception;

public class PetrelRpcException extends RuntimeException {
    public PetrelRpcException() {
    }

    public PetrelRpcException(String message) {
        super(message);
    }

    public PetrelRpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public PetrelRpcException(Throwable cause) {
        super(cause);
    }
}
