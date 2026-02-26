package rpc.petrel.test.server.filter;

import org.springframework.stereotype.Component;
import rpc.petrel.annotation.Activate;
import rpc.petrel.enums.TypeEnum;
import rpc.petrel.filter.Invocation;
import rpc.petrel.filter.PetrelFilter;

@Component
@Activate(group = TypeEnum.PROVIDER)
public class UserFilter implements PetrelFilter {
    @Override
    public void invoke(Invocation invocation) {
        String userId = invocation.getAttachment("user-id");
        System.out.println("user-id: " + userId);
    }
}
