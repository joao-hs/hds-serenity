package pt.ulisboa.tecnico.hdsledger.communication.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;

public class TransferResponse extends ClientResponse {

    private final String merkleRootHash;
    private final ArrayList<String> merkleProofPath;

    private final Collection<CommitMessage> proofOfConsensus;

    public TransferResponse(Status status, String clientRequestHash) {
        this.setGeneralStatus(GeneralStatus.NOT_SUBMITTED);
        this.setStatus(status);
        this.setClientRequestHash(clientRequestHash);
        this.merkleRootHash = null;
        this.merkleProofPath = null;
        this.proofOfConsensus = null;
    }

    public TransferResponse(Status status, String clientRequestHash, Pair<String, ArrayList<String>> proofOfInclusion, Collection<CommitMessage> proofOfConsensus) {
        this.setGeneralStatus(GeneralStatus.SUBMITTED);
        this.setStatus(status);
        this.setClientRequestHash(clientRequestHash);
        this.merkleProofPath = proofOfInclusion.getRight();
        this.merkleRootHash = proofOfInclusion.getLeft();
        this.proofOfConsensus = proofOfConsensus;
    }

    public Pair<String, ArrayList<String>> getProofOfInclusion() {
        return Pair.of(merkleRootHash, merkleProofPath);
    }

    public Collection<CommitMessage> getProofOfConsensus() {
        return proofOfConsensus;
    }

    @Override
    public int hashCode() {
        // since proofOfConsensus is an unordered collection, we need to make sure the order does not affect the hash
        int proofOfConsensusHashCode = 0;
        if (proofOfConsensus != null) {
            proofOfConsensusHashCode = proofOfConsensus.stream().mapToInt(CommitMessage::hashCode).sum();
        }
        return Objects.hash(getGeneralStatus(), getStatus(), getClientRequestHash(), merkleRootHash, merkleProofPath, proofOfConsensusHashCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TransferResponse) {
            TransferResponse other = (TransferResponse) obj;
            return getGeneralStatus().equals(other.getGeneralStatus())
                && getStatus().equals(other.getStatus())
                && getClientRequestHash().equals(other.getClientRequestHash())
                && (merkleRootHash == null || merkleRootHash.equals(other.merkleRootHash))
                && (merkleProofPath == null || merkleProofPath.equals(other.merkleProofPath))
                && (proofOfConsensus == null || proofOfConsensus.equals(other.proofOfConsensus));
        }
        return false;
    }
}
