package com.pazz.test.client.filter;

import rpc.petrel.annotation.Activate;
import rpc.petrel.enums.TypeEnum;
import rpc.petrel.filter.Invocation;
import rpc.petrel.filter.PetrelFilter;

@Activate(group = TypeEnum.CONSUMER)
public class UserPetrelFilter implements PetrelFilter {

    @Override
    public void invoke(Invocation invocation) {
        invocation.setAttachment("user-id", 1017002892456L + "");
    }
}
