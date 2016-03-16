package br.edu.ifsp.nfc_writer;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * <h1>NFC-Writer</h1>
 * Aplicativo para realizar, de forma simplificada, a escrita de textos no padrao Ndef em tags NFC
 * @author  Murilo Rodrigues
 * @version 1.0
 * @since   2016-03-15
 */
public class MainActivity extends AppCompatActivity {
    protected NfcAdapter nfcAdapter;
    private EditText edtTexto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        edtTexto = (EditText) findViewById(R.id.edtTexto);

        // Verifica se NFC está habilitado
        if (!hasNfc()) {
            Toast.makeText(this, R.string.nfc_disable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatch();
    }

    @Override
    protected void onPause(){
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    private	void enableForegroundDispatch()	{
        Intent intent =	new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent	= PendingIntent.getActivity(this, 0, intent, 0);

        // Da prioridade a esta activity quando alguma Tag for descoberta
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        // Se a intent for um NFC
        if (isNfcIntent(intent)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            // Escreve texto na tag NFC
            if (writeNdefMessage(edtTexto.getText().toString(), tag)) {
                Toast.makeText(this, R.string.write_ok, Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, R.string.write_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean writeNdefMessage(String text, Tag tag) {
        try {
            // Verifica se o texto foi informado
            if (text.isEmpty()) {
                throw new IllegalArgumentException(getResources().getString(R.string.text_empty));
            }
            NdefRecord[] records = new NdefRecord[]{};

            // Verifica se o Android está na versão 21 da API
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // Cria automaticamente registro do tipo texto no padrão Ndef
                records = new NdefRecord[]{NdefRecord.createTextRecord(null, text)};
            } else {
                // Cria manualmente registro do tipo texto no padrão Ndef
                records = new NdefRecord[]{createRecord(text)};
            }

            NdefMessage message = new NdefMessage(records);
            Ndef ndef = Ndef.get(tag);
            // Abre conexão I/O
            ndef.connect();
            try {

                // Verifica se tag permite ser escrita
                if (ndef.isWritable()) {
                    // Escreve na tag
                    ndef.writeNdefMessage(message);
                    return true;
                } else throw new IOException(getResources().getString(R.string.tag_writable));
            } finally {
                // Fecha conexão I/O
                ndef.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    boolean hasNfc() {
        boolean hasFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        boolean isEnabled = NfcAdapter.getDefaultAdapter(this).isEnabled();
        return hasFeature && isEnabled;
    }

    boolean	isNfcIntent(Intent	intent)	{
        // Verifica se a intent é uma tag NFC
        return	intent.hasExtra(NfcAdapter.EXTRA_TAG);
    }

    // Cria registro Ndef do tipo string (função já diponível na API 21 do Android)
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }
}
