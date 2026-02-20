package rpc.petrel.exception;

public class IllegalMethodNameException extends PetrelRpcException {
    public IllegalMethodNameException(String message) {
        super(message);
    }
}
