package com.Ray.Bicycle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Ray.Bicycle.R;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ConnectActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private final Set<BluetoothDevice> discoveredDevices = new HashSet<>();
    public String Address,Name;
    private RecyclerViewAdapter recyclerViewAdapter;
    public boolean isConnected = false;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                Log.d("onReceive", device.getName() + ":" + device.getAddress());

                discoveredDevices.add(device);
                updateList();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopDiscovery();
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                isConnected = true;
                System.out.println("conn");
                //Device is now connected
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                isConnected = false;
                System.out.println("disconn");
                //Device has disconnected
            }
        }
    };
    private Button buttonDiscovery;

    public String getDevName() {
        return Name;
    }
    public String getDevAddress(){
        return Address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_list);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());


        recyclerViewAdapter = new RecyclerViewAdapter();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(dividerItemDecoration);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("本裝置不支援藍芽功能")
                    .setCancelable(false)
                    .setMessage("本裝置不支援藍芽功能，程式即將結束。")
                    .setNeutralButton("結束", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        }

        buttonDiscovery = findViewById(R.id.btBTDiscover);
        buttonDiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(ConnectActivity.this, "android.permission.ACCESS_COARSE_LOCATION")) {
                    ActivityCompat.requestPermissions(ConnectActivity.this, new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 0);
                    return;
                }

                if (!bluetoothAdapter.isEnabled()) {
                    Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intentBluetoothEnable);
                    return;
                }
                discoverDevices();
            }
        });

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(broadcastReceiver, filter);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, filter);

        updateList();
    }
    public boolean isConnected(){
        return isConnected;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDiscovery();
    }

    private void startDiscovery() {
        bluetoothAdapter.startDiscovery();
        buttonDiscovery.setText("正在搜尋藍芽裝置…");
    }

    private void stopDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        buttonDiscovery.setText("搜尋附近的藍芽裝置");
    }

    private void discoverDevices() {
        stopDiscovery();

        discoveredDevices.clear();
        updateList();

        startDiscovery();
    }

    private void updateList() {
        recyclerViewAdapter.notifyDataSetChanged();
    }

    private class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView textName, textAddress;
        private BluetoothDevice device;
        private boolean isPaired;

        RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            textName = itemView.findViewById(R.id.textName);
            textAddress = itemView.findViewById(R.id.textAddress);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopDiscovery();
                    Name = device.getName();
                    Address = device.getAddress();
                    Intent intent = new Intent(ConnectActivity.this, MainActivity.class);
                    intent.putExtra("DeviceName", device.getName());
                    intent.putExtra("DeviceAddress", device.getAddress());
                    SharedPreferences BT = getSharedPreferences("BTDetail" , MODE_PRIVATE);
                    BT.edit()
                            .putString("Name" , Name)
                            .putString("Address" , Address)
                            .apply();
                    startActivity(intent);
                    finish();
                }
            });
        }
        /*public Observable.create(new ObservableOnSubscribe<Integer>() {
            String TAG = "TEST0";
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                Log.d(TAG, "=========================currentThread name: " + Thread.currentThread().getName());
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        })
        .subscribe(new Observer<Integer>() {
            String TAG = "TEST1";

            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "======================onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "======================onNext " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "======================onError");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "======================onComplete");
            }
        });*/
        public Observer observer = new Observer<Integer>() {
            String TAG = "test1";
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "======================onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "======================onNext " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "======================onError");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "======================onComplete");
            }
        };

        public Observer getObserver() {
            return observer;
        }

        void loadDevice(@NonNull BluetoothDevice device, boolean isPaired) {
            this.device = device;
            this.isPaired = isPaired;

            String name = this.device.getName();
            if (name == null) name = "尚未選擇裝置";

            icon.setImageResource(this.isPaired ? R.drawable.ic_bluetooth_black_24dp : R.drawable.ic_bluetooth_searching_black_24dp);
            textName.setText(name);
            textAddress.setText(this.device.getAddress());
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_item, parent, false);
            return new RecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (position < pairedDevices.size()) {
                holder.loadDevice(pairedDevices.toArray(new BluetoothDevice[0])[position], true);
            } else {
                holder.loadDevice(discoveredDevices.toArray(new BluetoothDevice[0])[position - pairedDevices.size()], false);
            }
        }

        @Override
        public int getItemCount() {
            return bluetoothAdapter.getBondedDevices().size() + discoveredDevices.size();
        }
    }

}
