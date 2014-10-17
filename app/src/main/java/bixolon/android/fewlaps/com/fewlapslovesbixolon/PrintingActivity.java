package bixolon.android.fewlaps.com.fewlapslovesbixolon;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PrintingActivity extends Activity {

    //The columns of your printer. We only tried the Bixolon 300 and the Bixolon 200II, so there are the values.
    //    private final int LINE_CHARS = 42 + 22; // Bixolon 300
    private final int LINE_CHARS = 42; // Bixolon 200II


    //Some time to don't flood the printer with new commands. It's fine to wait a little after sending an image to the printer.
    private static final long PRINTING_SLEEP_TIME = 300;

    //The time the printer takes to print the ticket. It makes the print button to be enabled again after this time in millis.
    //Of course, you can get it in an empiric way... :D
    private static final long PRINTING_TIME = 2200;

    //Two constants that some Bixolon printers send, but aren't included in the Bixolon library. Probably some printers can send it? Don't know.
    static final int MESSAGE_START_WORK = Integer.MAX_VALUE - 2;
    static final int MESSAGE_END_WORK = Integer.MAX_VALUE - 3;

    //The core of the monster: managing the Bixolon printer connection lifecycle
    private List<String> pairedPrinters = new ArrayList<String>();
    private Boolean connectedPrinter = false;
    private static BixolonPrinter bixolonPrinterApi;

    //View layer things
    private Animation rotation = null; //Caching an animation makes the world a better place to be
    private View layoutLoading;
    private View layoutThereArentPairedPrinters;
    private View layoutPrinterReady;
    private TextView debugTextView = null; //A hidden TextView where you can test things
    private Button printButton = null; //Guess it :P

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printing);

        findViewById(R.id.getSource).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/rocboronat/FewlapsLovesBixolon"));
                startActivity(i);
            }
        });

        if (rotation == null) {
            rotation = AnimationUtils.loadAnimation(this, R.anim.rotation);
        }

        debugTextView = (TextView) findViewById(R.id.debug);
        printButton = (Button) findViewById(R.id.print);

        layoutLoading = findViewById(R.id.layoutLoading);
        layoutThereArentPairedPrinters = findViewById(R.id.layoutNoExisteImpresora);
        layoutPrinterReady = findViewById(R.id.layoutImpresoraPreparada);

        printButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                printButton.setEnabled(false);
                new Handler().postDelayed(new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        printButton.setEnabled(true);
                    }
                }, PRINTING_TIME);

                Thread t = new Thread() {
                    /** Where the actual print happens. BTW, the easyest code. */
                    public void run() {
                        try {
                            //FIXME this example hardcodes the text values to increase a little the readability of the code. Don't do it in production! :)

                            bixolonPrinterApi.setSingleByteFont(BixolonPrinter.CODE_PAGE_858_EURO); //It fixes an issue printing special values like €, áéíóú...

                            bixolonPrinterApi.lineFeed(2, false); //It's like printing \n\n
                            Bitmap fewlapsBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fewlaps);

                            //BEWARE THE DOG: The 260 and 50 values are really MAGIC. They aren't as siple as width and height. It can break the Bitmap print.
                            bixolonPrinterApi.printBitmap(fewlapsBitmap, BixolonPrinter.ALIGNMENT_CENTER, 260, 50, false);

                            Thread.sleep(PRINTING_SLEEP_TIME); // Don't strees the printer while printing the Bitmap... it don't like it.

                            bixolonPrinterApi.lineFeed(2, false);
                            printText("Invoice\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.TEXT_ATTRIBUTE_FONT_A);

                            printTextTwoColumns("Name:", "Roc Boronat\n");
                            printTextTwoColumns("Company:", "Fewlaps\n");
                            printTextTwoColumns("Address 1:", "C/ Pujades 126\n");
                            printTextTwoColumns("Address 2:", "08018 Barcelona (Catalonia)\n");

                            bixolonPrinterApi.lineFeed(2, false);
                            bixolonPrinterApi.printQrCode("https://github.com/rocboronat/FewlapsLovesBixolon", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.QR_CODE_MODEL2, 5, false);
                            printText("Scan the QR\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.TEXT_ATTRIBUTE_FONT_A);
                            printText("and get the source!", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.TEXT_ATTRIBUTE_FONT_C);

                            bixolonPrinterApi.lineFeed(5, false);
                        } catch (Exception e) {
                            Log.e("ERROR", "Printing", e);
                        }
                    }
                };
                t.start();
            }
        });
        findViewById(R.id.pairPrinter).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (String mac : pairedPrinters) {
                    BluetoothUtil.unpairMac(mac);
                }
                pairedPrinters.clear();

                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(settingsIntent);
            }
        });

        updateScreenStatus(layoutLoading);
    }

    private void updateScreenStatus(View viewToShow) {
        if (viewToShow == layoutLoading) {
            layoutLoading.setVisibility(View.VISIBLE);
            layoutThereArentPairedPrinters.setVisibility(View.GONE);
            layoutPrinterReady.setVisibility(View.GONE);
            iconLoadingStart();
        } else if (viewToShow == layoutThereArentPairedPrinters) {
            layoutLoading.setVisibility(View.GONE);
            layoutThereArentPairedPrinters.setVisibility(View.VISIBLE);
            layoutPrinterReady.setVisibility(View.GONE);
            iconLoadingStop();
        } else if (viewToShow == layoutPrinterReady) {
            layoutLoading.setVisibility(View.GONE);
            layoutThereArentPairedPrinters.setVisibility(View.GONE);
            layoutPrinterReady.setVisibility(View.VISIBLE);
            iconLoadingStop();
        }

        updatePrintButtonState();
    }

    PairWithPrinterTask task = null;

    @Override
    protected void onResume() {
        super.onResume();

        bixolonPrinterApi = new BixolonPrinter(this, handler, null);
        task = new PairWithPrinterTask();
        task.execute();

        updatePrintButtonState();

        BluetoothUtil.startBluetooth();
    }

    @Override
    protected void onPause() {
        if (task != null) {
            task.stop();
            task = null;
        }

        if (bixolonPrinterApi != null) {
            bixolonPrinterApi.disconnect();
        }

        super.onPause();
    }

    private void updatePrintButtonState() {
        printButton.setEnabled(connectedPrinter != null && connectedPrinter == true);
    }

    private final Handler handler = new Handler() {
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            // Log.i("Handler", msg.what + " " + msg.arg1 + " " + msg.arg2);

            switch (msg.what) {

                case BixolonPrinter.MESSAGE_STATE_CHANGE:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_STATE_CHANGE");
                    switch (msg.arg1) {
                        case BixolonPrinter.STATE_CONNECTED:
                            updateScreenStatus(layoutPrinterReady);
                            Log.i("Handler", "BixolonPrinter.STATE_CONNECTED");
                            connectedPrinter = true;
                            updateScreenStatus(layoutPrinterReady);
                            break;

                        case BixolonPrinter.STATE_CONNECTING:
                            updateScreenStatus(layoutLoading);
                            Log.i("Handler", "BixolonPrinter.STATE_CONNECTING");
                            connectedPrinter = false;
                            break;

                        case BixolonPrinter.STATE_NONE:
                            updateScreenStatus(layoutLoading);
                            Log.i("Handler", "BixolonPrinter.STATE_NONE");
                            connectedPrinter = false;
                            break;
                    }
                    break;

                case BixolonPrinter.MESSAGE_WRITE:
                    switch (msg.arg1) {
                        case BixolonPrinter.PROCESS_SET_SINGLE_BYTE_FONT:
                            Log.i("Handler", "BixolonPrinter.PROCESS_SET_SINGLE_BYTE_FONT");
                            break;

                        case BixolonPrinter.PROCESS_SET_DOUBLE_BYTE_FONT:
                            Log.i("Handler", "BixolonPrinter.PROCESS_SET_DOUBLE_BYTE_FONT");
                            break;

                        case BixolonPrinter.PROCESS_DEFINE_NV_IMAGE:
                            Log.i("Handler", "BixolonPrinter.PROCESS_DEFINE_NV_IMAGE");
                            break;

                        case BixolonPrinter.PROCESS_REMOVE_NV_IMAGE:
                            Log.i("Handler", "BixolonPrinter.PROCESS_REMOVE_NV_IMAGE");
                            break;

                        case BixolonPrinter.PROCESS_UPDATE_FIRMWARE:
                            Log.i("Handler", "BixolonPrinter.PROCESS_UPDATE_FIRMWARE");
                            break;
                    }
                    break;

                case BixolonPrinter.MESSAGE_READ:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_READ");
                    break;

                case BixolonPrinter.MESSAGE_DEVICE_NAME:
                    debugTextView.setText(msg.getData().getString(BixolonPrinter.KEY_STRING_DEVICE_NAME));
                    Log.i("Handler", "BixolonPrinter.MESSAGE_DEVICE_NAME - " + msg.getData().getString(BixolonPrinter.KEY_STRING_DEVICE_NAME));
                    break;

                case BixolonPrinter.MESSAGE_TOAST:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_TOAST - " + msg.getData().getString("toast"));
                    // Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;

                // The list of paired printers
                case BixolonPrinter.MESSAGE_BLUETOOTH_DEVICE_SET:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_BLUETOOTH_DEVICE_SET");
                    if (msg.obj == null) {
                        updateScreenStatus(layoutThereArentPairedPrinters);
                    } else {
                        Set<BluetoothDevice> pairedDevices = (Set<BluetoothDevice>) msg.obj;
                        for (BluetoothDevice device : pairedDevices) {
                            if (!pairedPrinters.contains(device.getAddress())) {
                                pairedPrinters.add(device.getAddress());
                            }
                            if (pairedPrinters.size() == 1) {
                                PrintingActivity.bixolonPrinterApi.connect(pairedPrinters.get(0));
                            }
                        }
                    }
                    break;

                case BixolonPrinter.MESSAGE_PRINT_COMPLETE:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_PRINT_COMPLETE");
                    break;

                case BixolonPrinter.MESSAGE_COMPLETE_PROCESS_BITMAP:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_COMPLETE_PROCESS_BITMAP");
                    break;

                case MESSAGE_START_WORK:
                    Log.i("Handler", "MESSAGE_START_WORK");
                    break;

                case MESSAGE_END_WORK:
                    Log.i("Handler", "MESSAGE_END_WORK");
                    break;

                case BixolonPrinter.MESSAGE_USB_DEVICE_SET:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_USB_DEVICE_SET");
                    if (msg.obj == null) {
                        Toast.makeText(getApplicationContext(), "No connected device", Toast.LENGTH_SHORT).show();
                    } else {
                        // DialogManager.showUsbDialog(MainActivity.this,
                        // (Set<UsbDevice>) msg.obj, mUsbReceiver);
                    }
                    break;

                case BixolonPrinter.MESSAGE_NETWORK_DEVICE_SET:
                    Log.i("Handler", "BixolonPrinter.MESSAGE_NETWORK_DEVICE_SET");
                    if (msg.obj == null) {
                        Toast.makeText(getApplicationContext(), "No connectable device", Toast.LENGTH_SHORT).show();
                    }
                    // DialogManager.showNetworkDialog(PrintingActivity.this, (Set<String>) msg.obj);
                    break;
            }
        }
    };

    class PairWithPrinterTask extends AsyncTask<Void, Void, Void> {

        boolean running = true;

        public PairWithPrinterTask() {

        }

        public void stop() {
            running = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (running) {
                if (connectedPrinter == null || connectedPrinter == false) {
                    publishProgress();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        int action = 0;

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if (action < 20) {
                bixolonPrinterApi.findBluetoothPrinters();
                action++;
            } else {
                bixolonPrinterApi.disconnect();
                action = 0;
            }
        }
    }

    private void printText(String textToPrint) {
        printText(textToPrint, BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.TEXT_ATTRIBUTE_FONT_C);
    }

    private void printText(String textToPrint, int alignment) {
        printText(textToPrint, alignment, BixolonPrinter.TEXT_ATTRIBUTE_FONT_C);
    }

    private void printText(String textToPrint, int alignment, int attribute) {

        if (textToPrint.length() <= LINE_CHARS) {
            bixolonPrinterApi.printText(textToPrint, alignment, attribute, BixolonPrinter.TEXT_SIZE_HORIZONTAL1, false);
        } else {
            String textToPrintInNextLine = null;
            while (textToPrint.length() > LINE_CHARS) {
                textToPrintInNextLine = textToPrint.substring(0, LINE_CHARS);
                textToPrintInNextLine = textToPrintInNextLine.substring(0, textToPrintInNextLine.lastIndexOf(" ")).trim() + "\n";
                bixolonPrinterApi.printText(textToPrintInNextLine, alignment, attribute, BixolonPrinter.TEXT_SIZE_HORIZONTAL1, false);
                textToPrint = textToPrint.substring(textToPrintInNextLine.length(), textToPrint.length());
            }
            bixolonPrinterApi.printText(textToPrint, alignment, attribute, BixolonPrinter.TEXT_SIZE_HORIZONTAL1, false);
        }
    }

    /**
     * Print the common two columns ticket style text. Label+Value.
     *
     * @param leftText
     * @param rightText
     */
    private void printTextTwoColumns(String leftText, String rightText) {
        if (leftText.length() + rightText.length() + 1 > LINE_CHARS) { // If two Strings cannot fit in same line
            int alignment = BixolonPrinter.ALIGNMENT_LEFT;
            int attribute = 0;
            attribute |= BixolonPrinter.TEXT_ATTRIBUTE_FONT_C;
            bixolonPrinterApi.printText(leftText, alignment, attribute, BixolonPrinter.TEXT_SIZE_HORIZONTAL1, false);

            alignment = BixolonPrinter.ALIGNMENT_RIGHT;
            attribute = 0;
            attribute |= BixolonPrinter.TEXT_ATTRIBUTE_FONT_C;
            bixolonPrinterApi.printText(rightText, alignment, attribute, BixolonPrinter.TEXT_SIZE_HORIZONTAL1, false);
        } else {
            int padding = LINE_CHARS - leftText.length() - rightText.length();
            String paddingChar = " ";
            for (int i = 0; i < padding; i++) {
                paddingChar = paddingChar.concat(" ");
            }

            int alignment = BixolonPrinter.ALIGNMENT_LEFT;
            int attribute = 0;
            attribute |= BixolonPrinter.TEXT_ATTRIBUTE_FONT_C;
            bixolonPrinterApi.printText(leftText + paddingChar + rightText, alignment, attribute, BixolonPrinter.TEXT_SIZE_HORIZONTAL1, false);
        }
    }

    boolean animated = false;

    public void iconLoadingStart() {
        View loading = findViewById(R.id.loading);
        if (loading != null && !animated) {
            loading.startAnimation(rotation);
            loading.setVisibility(View.VISIBLE);
        }

        if (loading == null) {
            setProgressBarIndeterminateVisibility(Boolean.TRUE);
        }
        animated = true;
    }

    public void iconLoadingStop() {
        setProgressBarIndeterminateVisibility(Boolean.FALSE);

        View loading = findViewById(R.id.loading);
        if (loading != null) {
            loading.clearAnimation();
            loading.setVisibility(View.INVISIBLE);
        }
        animated = false;
    }
}
