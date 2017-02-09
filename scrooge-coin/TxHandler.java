import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPoolCopy;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        utxoPoolCopy = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<UTXO> spent_utxo = new ArrayList<UTXO>();
        double sum_input_values = 0.0;
        double sum_output_values = 0.0;

        int i = 0;
        for (Transaction.Input input : inputs) {
            UTXO u = new UTXO(input.prevTxHash, input.outputIndex);
            if (!(utxoPoolCopy.contains(u))) {
                return false;
            }
            Transaction.Output output = utxoPoolCopy.getTxOutput(u);
            if (!(Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature))) {
                return false;
            }
            if (spent_utxo.contains(u)) {
                return false;
            }
            spent_utxo.add(u);
            
            sum_input_values += utxoPoolCopy.getTxOutput(u).value;

            i += 1;
        }
        for (Transaction.Output output : outputs) {
            if (output.value < 0) {
                return false;
            }
            sum_output_values += output.value;
        }
        if (sum_input_values < sum_output_values) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // Transaction[] acceptedTxs = new Transaction[possibleTxs.size()];
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                acceptedTxs.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPoolCopy.removeUTXO(u);
                }
                int i = 0;
                for (Transaction.Output out : tx.getOutputs()) {
                    UTXO u = new UTXO(tx.getHash(), i);
                    utxoPoolCopy.addUTXO(u, out);
                    i += 1;
                }
            }
        }
        
        int arr_size = acceptedTxs.size();
        return acceptedTxs.toArray(new Transaction[arr_size]);
    }

}
