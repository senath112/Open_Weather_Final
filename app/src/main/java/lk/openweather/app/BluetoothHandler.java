package lk.openweather.app;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_SINT16;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT16;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT8;
import static com.welie.blessed.BluetoothBytesParser.bytes2String;
import static java.lang.Math.abs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.BondState;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.PhyOptions;
import com.welie.blessed.PhyType;
import com.welie.blessed.ScanFailure;
import com.welie.blessed.WriteType;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import timber.log.Timber;

class BluetoothHandler {

    // Intent constants
    public static final String MEASUREMENT_BLOODPRESSURE = "blessed.measurement.bloodpressure";
    public static final String MEASUREMENT_BLOODPRESSURE_EXTRA = "blessed.measurement.bloodpressure.extra";
    public static final String MEASUREMENT_TEMPERATURE = "blessed.measurement.temperature";
    public static final String MEASUREMENT_TEMPERATURE_EXTRA = "blessed.measurement.temperature.extra";
    public static final String MEASUREMENT_HEARTRATE = "blessed.measurement.heartrate";
    public static final String MEASUREMENT_HEARTRATE_EXTRA = "blessed.measurement.heartrate.extra";
    public static final String MEASUREMENT_GLUCOSE = "blessed.measurement.glucose";
    public static final String MEASUREMENT_GLUCOSE_EXTRA = "blessed.measurement.glucose.extra";
    public static final String MEASUREMENT_PULSE_OX = "blessed.measurement.pulseox";
    public static final String MEASUREMENT_PULSE_OX_EXTRA_CONTINUOUS = "blessed.measurement.pulseox.extra.continuous";
    public static final String MEASUREMENT_PULSE_OX_EXTRA_SPOT = "blessed.measurement.pulseox.extra.spot";
    public static final String MEASUREMENT_WEIGHT = "blessed.measurement.weight";
    public static final String MEASUREMENT_WEIGHT_EXTRA = "blessed.measurement.weight.extra";
    public static final String MEASUREMENT_EXTRA_PERIPHERAL = "blessed.measurement.peripheral";

    private static final UUID BLUETOOTH_LE_NRF_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID BLUETOOTH_LE_NRF_CHAR_WRITE = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); //read on microbit, write on adafruit
    private static final UUID BLUETOOTH_LE_NRF_CHAR_READ = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // read

    // UUIDs for the Blood Pressure service (BLP)
    private static final UUID BLP_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Health Thermometer service (HTS)
    private static final UUID HTS_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    private static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
    private static final UUID PNP_ID_CHARACTERISTIC_UUID = UUID.fromString("00002A50-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Heart Rate service (HRS)
    private static final UUID HRS_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HEARTRATE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Device Information service (DIS)
    private static final UUID DIS_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    private static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Current Time service (CTS)
    private static final UUID CTS_SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Battery Service (BAS)
    private static final UUID BTS_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Pulse Oximeter Service (PLX)
    public static final UUID PLX_SERVICE_UUID = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private static final UUID PLX_SPOT_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb");
    private static final UUID PLX_CONTINUOUS_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Weight Scale Service (WSS)
    public static final UUID WSS_SERVICE_UUID = UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb");
    private static final UUID WSS_MEASUREMENT_CHAR_UUID = UUID.fromString("00002A9D-0000-1000-8000-00805f9b34fb");

    public static final UUID GLUCOSE_SERVICE_UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002A52-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC_UUID = UUID.fromString("00002A34-0000-1000-8000-00805f9b34fb");

    // Contour Glucose Service
    public static final UUID CONTOUR_SERVICE_UUID = UUID.fromString("00000000-0002-11E2-9E96-0800200C9A66");
    private static final UUID CONTOUR_CLOCK = UUID.fromString("00001026-0002-11E2-9E96-0800200C9A66");

    // Local variables
    public BluetoothCentralManager central;
    private static BluetoothHandler instance = null;
    private final Context context;
    private final Handler handler = new Handler();
    private int currentTimeCounter = 0;
    private BluetoothPeripheral bluetoothPeripheral;

    // Callback for peripherals
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            // Request a higher MTU, iOS always asks for 185
            peripheral.requestMtu(185);

            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);

            peripheral.setPreferredPhy(PhyType.LE_2M, PhyType.LE_2M, PhyOptions.S2);

            // Read manufacturer and model number fr\om the Device Information Service
            peripheral.readCharacteristic(DIS_SERVICE_UUID, MANUFACTURER_NAME_CHARACTERISTIC_UUID);
            peripheral.readCharacteristic(DIS_SERVICE_UUID, MODEL_NUMBER_CHARACTERISTIC_UUID);

            peripheral.readPhy();

            // Turn on notifications for Current Time Service and write it if possible
            BluetoothGattCharacteristic currentTimeCharacteristic = peripheral.getCharacteristic(CTS_SERVICE_UUID, CURRENT_TIME_CHARACTERISTIC_UUID);
            if (currentTimeCharacteristic != null) {
                peripheral.setNotify(currentTimeCharacteristic, true);

                // If it has the write property we write the current time
                if ((currentTimeCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                    // Write the current time unless it is an Omron device
                    if (!isOmronBPM(peripheral.getName())) {
                        BluetoothBytesParser parser = new BluetoothBytesParser();
                        parser.setCurrentTime(Calendar.getInstance());
                        peripheral.writeCharacteristic(currentTimeCharacteristic, parser.getValue(), WriteType.WITH_RESPONSE);
                    }
                }
            }

            // Try to turn on notifications for other characteristics
            peripheral.readCharacteristic(BTS_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID);
            peripheral.setNotify(BLP_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(HTS_SERVICE_UUID, TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(HRS_SERVICE_UUID, HEARTRATE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(PLX_SERVICE_UUID, PLX_CONTINUOUS_MEASUREMENT_CHAR_UUID, true);
            peripheral.setNotify(PLX_SERVICE_UUID, PLX_SPOT_MEASUREMENT_CHAR_UUID, true);
            peripheral.setNotify(WSS_SERVICE_UUID, WSS_MEASUREMENT_CHAR_UUID, true);
            peripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(CONTOUR_SERVICE_UUID, CONTOUR_CLOCK, true);
            peripheral.setNotify(BLUETOOTH_LE_NRF_SERVICE, BLUETOOTH_LE_NRF_CHAR_READ, true);

        }

        @Override
        public void onNotificationStateUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                final boolean isNotifying = peripheral.isNotifying(characteristic);
                Timber.i("SUCCESS: Notify set to '%s' for %s", isNotifying, characteristic.getUuid());
                if (characteristic.getUuid().equals(CONTOUR_CLOCK)) {
                    writeContourClock(peripheral);
                } else if (characteristic.getUuid().equals(GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID)) {
                    writeGetAllGlucoseMeasurements(peripheral);
                }
            } else {
                Timber.e("ERROR: Changing notification state failed for %s (%s)", characteristic.getUuid(), status);
            }
        }

        @Override
        public void onCharacteristicWrite(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                Timber.i("SUCCESS: Writing <%s> to <%s>", bytes2String(value), characteristic.getUuid());
            } else {
                Timber.i("ERROR: Failed writing <%s> to <%s> (%s)", bytes2String(value), characteristic.getUuid(), status);
            }
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status != GattStatus.SUCCESS) {
                Log.i("BluetoothHandler1",new String(value));
                return;
            }


            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);
            if (characteristicUUID.equals(BLUETOOTH_LE_NRF_CHAR_READ)) {
                Log.i("BluetoothHandler",new String(value));
                String json = new String(value);
                Gson gson = new Gson();
                SplitData split = gson.fromJson(json, SplitData.class);
                System.out.println(split);
            }


        }

        @Override
        public void onMtuChanged(@NotNull BluetoothPeripheral peripheral, int mtu, @NotNull GattStatus status) {
            Timber.i("new MTU set: %d", mtu);
        }

        private void sendMeasurement(@NotNull Intent intent, @NotNull BluetoothPeripheral peripheral ) {
            intent.putExtra(MEASUREMENT_EXTRA_PERIPHERAL, peripheral.getAddress());
            context.sendBroadcast(intent);
        }

        private void writeContourClock(@NotNull BluetoothPeripheral peripheral) {
            Calendar calendar = Calendar.getInstance();
            int offsetInMinutes = calendar.getTimeZone().getRawOffset() / 60000;
            int dstSavingsInMinutes = calendar.getTimeZone().getDSTSavings() / 60000;
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
            parser.setIntValue(1, FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.YEAR), FORMAT_UINT16);
            parser.setIntValue(calendar.get(Calendar.MONTH) + 1, FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.DAY_OF_MONTH), FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.HOUR_OF_DAY), FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.MINUTE), FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.SECOND), FORMAT_UINT8);
            parser.setIntValue(offsetInMinutes + dstSavingsInMinutes, FORMAT_SINT16);
            peripheral.writeCharacteristic(CONTOUR_SERVICE_UUID, CONTOUR_CLOCK, parser.getValue(), WriteType.WITH_RESPONSE);
        }

        private void writeGetAllGlucoseMeasurements(@NotNull BluetoothPeripheral peripheral) {
            byte OP_CODE_REPORT_STORED_RECORDS = 1;
            byte OPERATOR_ALL_RECORDS = 1;
            final byte[] command = new byte[] {OP_CODE_REPORT_STORED_RECORDS, OPERATOR_ALL_RECORDS};
            peripheral.writeCharacteristic(GLUCOSE_SERVICE_UUID, GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID, command, WriteType.WITH_RESPONSE);
        }
    };

    // Callback for central
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            Log.i("connected to '%s'", peripheral.getName());
            bluetoothPeripheral = peripheral;
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Timber.e("connection '%s' failed with status %s", peripheral.getName(), status);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Timber.i("disconnected '%s' with status %s", peripheral.getName(), status);
            bluetoothPeripheral = null;

            // Reconnect to this device when it becomes available again
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    central.autoConnectPeripheral(peripheral, peripheralCallback);
                }
            }, 5000);
        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            Timber.i("Found peripheral '%s'", peripheral.getName());
            central.stopScan();

            if (peripheral.getName().contains("Contour") && peripheral.getBondState() == BondState.NONE) {
                // Create a bond immediately to avoid double pairing popups
                //central.createBond(peripheral, peripheralCallback);
            } else {
                central.connectPeripheral(peripheral, peripheralCallback);
                Log.i("BluetoothHandler",peripheral.hashCode()+"");
            }
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            Timber.i("bluetooth adapter changed state to %d", state);
            if (state == BluetoothAdapter.STATE_ON) {
                // Bluetooth is on now, start scanning again
                // Scan for peripherals with a certain service UUIDs
                central.startPairingPopupHack();
                startScan();
            }
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            Timber.i("scanning failed with error %s", scanFailure);
        }
    };

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context.getApplicationContext());
        }
        return instance;
    }

    private BluetoothHandler(Context context) {
        this.context = context;

        // Plant a tree
        //Timber.plant(new Timber.DebugTree());

        // Create BluetoothCentral
        central = new BluetoothCentralManager(context, bluetoothCentralManagerCallback, new Handler());

        // Scan for peripherals with a certain service UUIDs
        central.startPairingPopupHack();
        startScan();
    }

    private void startScan() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //central.scanForPeripheralsWithServices(new UUID[]{BLP_SERVICE_UUID, HTS_SERVICE_UUID, PLX_SERVICE_UUID, WSS_SERVICE_UUID, GLUCOSE_SERVICE_UUID});
                central.scanForPeripheralsWithNames(new String[]{"ගවේෂ 1.0"});

            }
        },1000);
    }

    private boolean isOmronBPM(final String name) {
        return name.contains("BLESmart_") || name.contains("BLEsmart_");
    }
    public void readDataFromEsp32() {
        String[] codeLines = new String[]{"import os","f=open('datalog.d/datalog')","print(f.read())","machine.SOFT_RESET"};
        for (String codeLine :
                codeLines) {
            final byte[] command = (codeLine + "\r\n").getBytes();
            if (bluetoothPeripheral == null) {
                Log.d("BluetoothHandler", "Not Connected");
                return;
            }
            bluetoothPeripheral.writeCharacteristic(BLUETOOTH_LE_NRF_SERVICE, BLUETOOTH_LE_NRF_CHAR_WRITE, command, WriteType.WITH_RESPONSE);
            Log.d("BluetoothHandler", "Write Success");
            Log.i("BluetoothHandler",bluetoothPeripheral.hashCode()+"");
        }
    }
    class SplitData {

        private Integer temperature;
        private Integer humidity;
        private Integer pressure;
        private Integer timestamp;


    }

}