package com.contractiq.fsm;

import com.contractiq.domain.contract.ContractEvent;
import com.contractiq.domain.contract.ContractStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<ContractStatus, ContractEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ContractStatus, ContractEvent> states) throws Exception
    {
        states.withStates().initial(ContractStatus.DRAFT).states(EnumSet.allOf(ContractStatus.class));
    }

    public void configure(StateMachineTransitionConfigurer<ContractStatus, ContractEvent> transitions) throws Exception
    {
         transitions.withExternal().source(ContractStatus.DRAFT).target(ContractStatus.UNDER_REVIEW).event(ContractEvent.SUBMIT_FOR_REVIEW)
                 .and()
                 .withExternal().source(ContractStatus.UNDER_REVIEW).target(ContractStatus.APPROVED).event(ContractEvent.APPROVE)
                 .and()
                 .withExternal().source(ContractStatus.APPROVED).target(ContractStatus.SIGNED).event(ContractEvent.ACTIVATE).event(ContractEvent.SIGN)
                 .and()
                 .withExternal().source(ContractStatus.SIGNED).target(ContractStatus.ACTIVE).event(ContractEvent.ACTIVATE)
                 .and()
                 .withExternal().source(ContractStatus.ACTIVE).target(ContractStatus.TERMINATED).event(ContractEvent.TERMINATE);
    }


}
