package com.guoxiaoxing.android.sdk.design._08_responsiblilty_chain_pattern;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:52
 */
public class Client {

    public static void main(String[] args) {

        GroupLeader groupLeader = new GroupLeader();
        ManagerLeader managerLeader = new ManagerLeader();
        BossLeader bossLeader = new BossLeader();

        groupLeader.nextHandler = managerLeader;
        managerLeader.nextHandler = bossLeader;

        groupLeader.handlerRequest(4000);
    }
}
