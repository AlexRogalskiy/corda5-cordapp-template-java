package net.corda.c5.sample.landregistry.flows;

import net.corda.c5.sample.landregistry.contracts.LandContract;
import net.corda.c5.sample.landregistry.states.LandState;
import net.corda.systemflows.CollectSignaturesFlow;
import net.corda.systemflows.FinalityFlow;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.flows.flowservices.FlowEngine;
import net.corda.v5.application.flows.flowservices.FlowIdentity;
import net.corda.v5.application.flows.flowservices.FlowMessaging;
import net.corda.v5.application.identity.CordaX500Name;
import net.corda.v5.application.identity.Party;
import net.corda.v5.application.injection.CordaInject;
import net.corda.v5.application.services.IdentityService;
import net.corda.v5.application.services.json.JsonMarshallingService;
import net.corda.v5.application.services.persistence.PersistenceService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.stream.Cursor;
import net.corda.v5.ledger.contracts.StateAndRef;
import net.corda.v5.ledger.services.NotaryLookupService;
import net.corda.v5.ledger.services.vault.SetBasedVaultQueryFilter;
import net.corda.v5.ledger.services.vault.StateStatus;
import net.corda.v5.ledger.transactions.SignedTransaction;
import net.corda.v5.ledger.transactions.SignedTransactionDigest;
import net.corda.v5.ledger.transactions.TransactionBuilder;
import net.corda.v5.ledger.transactions.TransactionBuilderFactory;

import java.time.Duration;
import java.util.*;

@InitiatingFlow
@StartableByRPC
public class TransferLandFlow implements Flow<SignedTransactionDigest> {
    private RpcStartFlowRequestParameters params;

    @JsonConstructor
    public TransferLandFlow(RpcStartFlowRequestParameters params) {
        this.params = params;
    }

    @CordaInject
    private FlowEngine flowEngine;

    @CordaInject
    private FlowIdentity flowIdentity;

    @CordaInject
    private FlowMessaging flowMessaging;

    @CordaInject
    private TransactionBuilderFactory transactionBuilderFactory;

    @CordaInject
    private IdentityService identityService;

    @CordaInject
    private NotaryLookupService notaryLookupService;

    @CordaInject
    private JsonMarshallingService jsonMarshallingService;

    @CordaInject
    private PersistenceService persistenceService;

    @Override
    @Suspendable
    public SignedTransactionDigest call() {

        Party notary = notaryLookupService.getNotaryIdentities().get(0);

        Map<String, String> parametersMap = jsonMarshallingService.parseJson(params.getParametersInJson(), Map.class);
        if(parametersMap.get("plotNumber") == null)
            throw new BadRpcStartFlowRequestException("Missing Parameter: \"plotNumber\" ");

        if(parametersMap.get("owner") == null)
            throw new BadRpcStartFlowRequestException("Missing Parameter: \"owner\" ");

        String plotNumber = parametersMap.get("plotNumber");
        Party owner = identityService.partyFromName(CordaX500Name.parse(parametersMap.get("owner")));


        //Query the landState
        Map <String, Object> namedParameters = new LinkedHashMap<>();
        namedParameters.put("stateStatus", StateStatus.UNCONSUMED);
        Cursor<StateAndRef<LandState>> cursor = persistenceService.query(
                "VaultState.findByStateStatus",
                namedParameters,
                new SetBasedVaultQueryFilter.Builder()
                        .withContractStateClassNames(Set.of(LandState.class.getName()))
                        .build(),
                "Corda.IdentityStateAndRefPostProcessor"
        );

        List<StateAndRef<LandState>> inputLandStateStateAndRefList =
                cursor.poll(100, Duration.ofSeconds(20)).getValues();

        StateAndRef<LandState> inputLandStateStateAndRef =
                inputLandStateStateAndRefList.stream().filter(stateAndRef -> {
            LandState landState = stateAndRef.getState().getData();
            return landState.getPlotNumber().equals(plotNumber);
        }).findAny().orElseThrow(() -> new FlowException("Land Not Found"));

        LandState outputLandState = getOutputState(inputLandStateStateAndRef, owner);

        // Build the transaction.
        TransactionBuilder transactionBuilder = transactionBuilderFactory.create()
                .setNotary(inputLandStateStateAndRef.getState().getNotary())
                .addInputState(inputLandStateStateAndRef)
                .addOutputState(outputLandState)
                .addCommand(new LandContract.Commands.Transfer(),
                        Arrays.asList(outputLandState.getOwner().getOwningKey(),
                                inputLandStateStateAndRef.getState().getData().getIssuer().getOwningKey(),
                                inputLandStateStateAndRef.getState().getData().getOwner().getOwningKey()));

        // Verify that the transaction is valid.
        transactionBuilder.verify();

        // Self Sign the transaction.
        SignedTransaction selfSignedTx = transactionBuilder.sign();

        // Send the state to the counterparty, and receive their signature.
        FlowSession ownerSession = flowMessaging.initiateFlow(outputLandState.getOwner());
        FlowSession issuerSession = flowMessaging.initiateFlow(inputLandStateStateAndRef.getState().getData().getIssuer());
        SignedTransaction fullySignedTx = flowEngine.subFlow(new CollectSignaturesFlow(selfSignedTx,
                Arrays.asList(ownerSession, issuerSession)));

        // Notarise and record the transaction. Add issuer session so that they get a copy of the transaction.
        SignedTransaction notarisedTx;
            notarisedTx = flowEngine.subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(ownerSession, issuerSession)));

        // Return Json output
        return new SignedTransactionDigest(notarisedTx.getId(),
                Collections.singletonList(jsonMarshallingService.formatJson(notarisedTx.getTx().getOutputStates().get(0))),
                notarisedTx.getSigs());
    }

    private LandState getOutputState(StateAndRef<LandState> inputStateAndRef, Party owner){
        LandState inputState = inputStateAndRef.getState().getData();
        LandState landState = new LandState(inputState.getPlotNumber(), inputState.getDimensions(),
                inputState.getArea(), owner, inputState.getIssuer());
        return landState;
    }
}




