package wallettemplate;

import javafx.application.Application;
import javafx.stage.Stage;
import wallettemplate.wallets.bitcoin.Bitcoin;
import wallettemplate.wallets.dogecoin.Dogecoin;

public class Main {
    /**
        TODO:
        -- start(stage) instead of main(args)...
    */

    public static void main(String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("bitcoin")) {
                Bitcoin.main(args);
            } else if (args[0].equalsIgnoreCase("dogecoin")) {
                Dogecoin.main(args);
            } else {
                System.out.println("Unknown network: '" + args[0] + "'\n\nTry using 'bitcoin' or 'dogecoin' ...");
            }
        } else {
            System.out.println("Usage: wallettemplate <bitcoin|dogecoin> [<wallet_name>]");
        }
        //javafx.application.Application.launch(Bitcoin.class, args);
        //javafx.application.Application.launch(Dogecoin.class);
        //launch(args);
    }

    /*
    @Override
    public void start(Stage stage) throws Exception {
        // ...
    }
    */

}
