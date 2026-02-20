package rpc.petrel.exception;

public class FutureResultGetException extends PetrelRpcException {
    public FutureResultGetException(String message) {
        super(message);
    }

    public FutureResultGetException(Throwable cause) {
        super(cause);
    }
}
