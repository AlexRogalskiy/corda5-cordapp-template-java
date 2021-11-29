package net.corda.c5template.states;

import com.google.gson.Gson;
import net.corda.c5template.contracts.TemplateContract;
import net.corda.v5.application.identity.AbstractParty;
import net.corda.v5.application.identity.Party;
import net.corda.v5.application.utilities.JsonRepresentable;
import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.ledger.contracts.BelongsToContract;
import net.corda.v5.ledger.contracts.ContractState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@CordaSerializable
@BelongsToContract(TemplateContract.class)
public class TemplateState implements ContractState, JsonRepresentable {

    private String msg;
    private Party sender;
    private Party receiver;

    /* Constructor of your Corda state */
    public TemplateState(String msg, Party sender, Party receiver) {
        this.msg = msg;
        this.sender = sender;
        this.receiver = receiver;
    }

    //getters
    public String getMsg() {
        return msg;
    }

    public Party getSender() {
        return sender;
    }

    public Party getReceiver() {
        return receiver;
    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver);
    }

    private TemplateStateDtoJava toDto() {
        return new TemplateStateDtoJava(
                msg,
                sender.getName().toString(),
                receiver.getName().toString()
        );
    }

    @NotNull
    @Override
    public String toJsonString() {
        return new Gson().toJson(this.toDto());
    }

    static class TemplateStateDtoJava {
        private String msg;
        private String sender;
        private String receiver;

        public TemplateStateDtoJava() {
        }

        public TemplateStateDtoJava(String msg, String sender, String receiver) {
            this.msg = msg;
            this.sender = sender;
            this.receiver = receiver;
        }

        public String getMsg() {
            return msg;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public void setReceiver(String receiver) {
            this.receiver = receiver;
        }
    }


}