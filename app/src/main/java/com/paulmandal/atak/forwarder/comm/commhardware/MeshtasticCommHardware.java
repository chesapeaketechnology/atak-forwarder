package com.paulmandal.atak.forwarder.comm.commhardware;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.MessageStatus;
import com.geeksville.mesh.NodeInfo;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.AddToGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.CreateGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;

import java.util.concurrent.CountDownLatch;

public class MeshtasticCommHardware extends MessageLengthLimitedCommHardware {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticCommHardware.class.getSimpleName();

    /**
     * Intents the Meshtastic service can send
     */
    private static final String ACTION_MESH_CONNECTED = "com.geeksville.mesh.MESH_CONNECTED";
    private static final String ACTION_RECEIVED_DATA = "com.geeksville.mesh.RECEIVED_DATA";
    private static final String ACTION_NODE_CHANGE = "com.geeksville.mesh.NODE_CHANGE";
    private static final String ACTION_MESSAGE_STATUS = "com.geeksville.mesh.MESSAGE_STATUS";

    /**
     * Extra data fields from the Meshtastic service
     */
    // a bool true means now connected, false means not
    private static final String EXTRA_CONNECTED = "com.geeksville.mesh.Connected";

    /// a bool true means we expect this condition to continue until, false means device might come back
    private static final String EXTRA_PERMANENT = "com.geeksville.mesh.Permanent";

    private static final String EXTRA_PAYLOAD = "com.geeksville.mesh.Payload";
    private static final String EXTRA_NODEINFO = "com.geeksville.mesh.NodeInfo";
    private static final String EXTRA_PACKET_ID = "com.geeksville.mesh.PacketId";
    private static final String EXTRA_STATUS = "com.geeksville.mesh.Status";

    private Activity mActivity;

    IMeshService mMeshService;
    private ServiceConnection mServiceConnection;
    private CountDownLatch mPendingMessageCountdownLatch; // TODO: maybe move this up to MessageLengthLimitedCommHardware
    private int mPendingMessageId;
    private boolean mPendingMessageReceived;

    boolean mBound = false;

    public MeshtasticCommHardware(Handler handler,
                                  GroupTracker groupTracker,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  Activity activity) {
        super(handler, commandQueue, queuedCommandFactory, groupTracker);

        mActivity = activity;

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mBound = true;
                setConnected(true);
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;
                setConnected(false);
            }
        };

        Intent intent = new Intent();
        intent.setClassName("com.geeksville.mesh","com.geeksville.mesh.service.MeshService");
        activity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);
        filter.addAction(ACTION_NODE_CHANGE);
        filter.addAction(ACTION_RECEIVED_DATA);
        filter.addAction(ACTION_MESSAGE_STATUS);

        mActivity.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected boolean sendMessageSegment(byte[] message, long targetId) {
        Log.d(TAG, "sendMessageSegment");

        prepareToSendMessage();

        DataPacket dataPacket = new DataPacket("^all", message); // TODO: null instead of "^all" ?
        try {
            mMeshService.send(dataPacket);
            mPendingMessageId = dataPacket.getId();
            Log.d(TAG, "send message id: " + dataPacket.getId());
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessageSegment, RemoteException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        awaitPendingMessageCountDownLatch();

        return mPendingMessageReceived;
    }

    @Override
    public boolean isBatteryCharging() {
        return false;
    }

    @Override
    public Integer getBatteryChargePercentage() {
        return null;
    }

    @Override
    protected void handleScanForCommDevice() {
        // TODO: handle connnect/disconnect in plugin
    }

    @Override
    protected void handleDisconnectFromCommDevice() {
        // TODO: handle connnect/disconnect in plugin
    }

    @Override
    protected void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        // TODO: handle group creation in plugin
    }

    @Override
    protected void handleCreateGroup(CreateGroupCommand createGroupCommand) {
        // TODO: handle group creation in plugin
    }

    @Override
    protected void handleAddToGroup(AddToGroupCommand addToGroupCommand) {
        // TODO: handle group creation in plugin
    }

    @Override
    protected void handleGetBatteryStatus() {
        // TODO: handle group creation in plugin
    }

    @Override
    public void destroy() {
        super.destroy();
        mActivity.unbindService(mServiceConnection);
        mActivity.unregisterReceiver(mBroadcastReceiver);
        mBound = false;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);

            if (action == null) {
                Log.e(TAG, "onReceive, action was null");
                return;
            }

            switch (action) {
                case ACTION_MESH_CONNECTED:
                    Log.d(TAG, "ACTION_MESH_CONNECTED: " + intent.getStringExtra(EXTRA_CONNECTED));
                    setConnected(true);
                    break;
                case ACTION_NODE_CHANGE:
                    NodeInfo nodeInfo = intent.getParcelableExtra(EXTRA_NODEINFO);
                    Log.d(TAG, "ACTION_NODE_CHANGE, info: " + nodeInfo);
                    break;
                case ACTION_MESSAGE_STATUS:
                    Log.d(TAG, "ACTION_MESSAGE_STATUS");

                    int id = intent.getIntExtra(EXTRA_PACKET_ID, 0);
                    MessageStatus status = intent.getParcelableExtra(EXTRA_STATUS);

                    Log.d(TAG, "id: " + id + " status: " + status);

                    handleMessageStatusChange(id, status);
                    break;
                case ACTION_RECEIVED_DATA:
                    Log.d(TAG, "ACTION_RECEIVED_DATA");

                    DataPacket payload = intent.getParcelableExtra(EXTRA_PAYLOAD);

                    if (payload.getDataType() == MeshProtos.Data.Type.CLEAR_TEXT_VALUE) {
                        handleMessageChunk(0, payload.getBytes()); // TODO: fix sender id
                    } else {
                        Log.e(TAG, "Unknown payload type: " + payload.getDataType());
                    }
                    break;
                default:
                    Log.e(TAG, "Do not know how to handle intent action: " + intent.getAction());
                    break;
            }
        }
    };

    private void handleMessageStatusChange(int id, MessageStatus status) {
        if (id != mPendingMessageId) {
            Log.e(TAG, "handleMessageStatusChange for a msg we don't care about id: " + id + " status: " + status);
            return;
        }

        // TODO: fix this, for some reason the MeshService is reporting ERROR for all msgs but they are actually going through
        mPendingMessageReceived = true; // status != MessageStatus.ERROR;

        if (status == MessageStatus.ERROR || status == MessageStatus.DELIVERED) {
            mPendingMessageCountdownLatch.countDown();
        }
    }

    private void prepareToSendMessage() {
        mPendingMessageCountdownLatch = new CountDownLatch(1);
    }

    private void awaitPendingMessageCountDownLatch() {
        try {
            mPendingMessageCountdownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
