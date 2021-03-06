package wallettemplate.wallets.dogecoin;

import com.aquafx_project.AquaFx;
import com.google.dogecoin.core.NetworkParameters;
import com.google.dogecoin.kits.WalletAppKit;
import com.google.dogecoin.params.MainNetParams;
import com.google.dogecoin.store.BlockStoreException;
import com.google.dogecoin.utils.BriefLogFormatter;
import com.google.dogecoin.utils.Threading;
import com.google.common.base.Throwables;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.wallets.dogecoin.utils.GuiUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static wallettemplate.wallets.dogecoin.utils.GuiUtils.*;

public class Dogecoin extends Application {
    public static String APP_NAME = "Dogecoin";

    public static String COIN_NAME = "dogecoin";
    public static String WALLET_NAME = "default";
    public static String USER_DIR = ".";

    public static NetworkParameters params = MainNetParams.get();
    public static WalletAppKit kit;
    public static Dogecoin instance;

    private StackPane uiStack;
    private Pane mainUI;

    @Override
    public void start(Stage mainWindow) throws Exception {
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();
        try {
            init(mainWindow);
        } catch (Throwable t) {
            // Nicer message for the case where the block store file is locked.
            if (Throwables.getRootCause(t) instanceof BlockStoreException) {
                GuiUtils.informationalAlert("Already running", "This application is already running and cannot be started twice.");
            } else {
                throw t;
            }
        }
    }

    private void init(Stage mainWindow) throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            AquaFx.style();
        }
        // Load the GUI. The Controller class will be automagically created and wired up.
        URL location = getClass().getResource("dogecoin.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        mainUI = loader.load();
        DogecoinController controller = loader.getController();
        // Configure the window with a StackPane so we can overlay things on top of the main UI.
        uiStack = new StackPane(mainUI);
        mainWindow.setTitle(APP_NAME + " [" + WALLET_NAME + "] - FX Application");
        final Scene scene = new Scene(uiStack);
        TextFieldValidator.configureScene(scene);   // Add CSS that we need.
        mainWindow.setScene(scene);

        // Make log output concise.
        BriefLogFormatter.init();
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        kit = new WalletAppKit(params, new File(USER_DIR+"/"+COIN_NAME), COIN_NAME+"-"+WALLET_NAME);
        if (params == MainNetParams.get()) {
            // Checkpoints are block headers that ship inside our app: for a new user, we pick the last header
            // in the checkpoints file and then download the rest from the network. It makes things much faster.
            // Checkpoint files are made using the BuildCheckpoints tool and usually we have to download the
            // last months worth or more (takes a few seconds).
            try {
                kit.setCheckpoints(getClass().getResourceAsStream("checkpoints"));
            } catch (NullPointerException e) {
                System.out.println("No " + COIN_NAME + ".checkpoints file. Loading chain...");
            }
        }

        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.
        kit.setDownloadListener(controller.progressBarUpdater())
               .setBlockingStartup(false)
               .setUserAgent(APP_NAME, "1.0")
               .startAndWait();
        // Don't make the user wait for confirmations for now, as the intention is they're sending it their own money!
        kit.wallet().allowSpendingUnconfirmedTransactions();
        kit.peerGroup().setMaxConnections(11);
        System.out.println(kit.wallet());
        controller.onBitcoinSetup();
        mainWindow.show();
    }

    public class OverlayUI<T> {
        public Node ui;
        public T controller;

        public OverlayUI(Node ui, T controller) {
            this.ui = ui;
            this.controller = controller;
        }

        public void show() {
            blurOut(mainUI);
            uiStack.getChildren().add(ui);
            fadeIn(ui);
        }

        public void done() {
            checkGuiThread();
            fadeOutAndRemove(ui, uiStack);
            blurIn(mainUI);
            this.ui = null;
            this.controller = null;
        }
    }

    public <T> OverlayUI<T> overlayUI(Node node, T controller) {
        checkGuiThread();
        OverlayUI<T> pair = new OverlayUI<T>(node, controller);
        // Auto-magically set the overlayUi member, if it's there.
        try {
            controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        pair.show();
        return pair;
    }

    /** Loads the FXML file with the given name, blurs out the main UI and puts this one on top. */
    public <T> OverlayUI<T> overlayUI(String name) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = getClass().getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            OverlayUI<T> pair = new OverlayUI<T>(ui, controller);
            // Auto-magically set the overlayUi member, if it's there.
            try {
                controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
            }
            pair.show();
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    @Override
    public void stop() throws Exception {
        kit.stopAndWait();
        super.stop();
    }

    public static void main(String[] args) {
        if (args.length >= 2) {
            WALLET_NAME = args[1];
        }
        launch(args);
    }
}
