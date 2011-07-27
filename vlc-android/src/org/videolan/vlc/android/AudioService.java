package org.videolan.vlc.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class AudioService extends Service {
	private static final String TAG = "VLC/AudioService";
	
	private static final int SHOW_PROGRESS = 0;
	
	private LibVLC mLibVLC;
	private ArrayList<Media> mMediaList;
	private Media mCurrentMedia;
	private ArrayList<IAudioServiceCallback> mCallback;
	private EventManager mEventManager;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Get libVLC instance
        try {
			mLibVLC = LibVLC.getInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
		}		
		
		mCallback = new ArrayList<IAudioServiceCallback>();
		mMediaList = new ArrayList<Media>();
		mEventManager = EventManager.getIntance();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mInterface;
	}

	
    /**
     *  Handle libvlc asynchronous events 
     */
    private Handler mEventHandler = new Handler() {

		@Override
        public void handleMessage(Message msg) {
            switch (msg.getData().getInt("event")) {
                case EventManager.MediaPlayerPlaying:
                    Log.e(TAG, "MediaPlayerPlaying");
                    break;
                case EventManager.MediaPlayerPaused:
                    Log.e(TAG, "MediaPlayerPaused");
                    executeUpdate();
                    // also hide notification if phone ringing
                    hideNotification(); 
                    break;
                case EventManager.MediaPlayerStopped:
                    Log.e(TAG, "MediaPlayerStopped");
                    executeUpdate();
                    break;
                case EventManager.MediaPlayerEndReached:
                    Log.e(TAG, "MediaPlayerEndReached");
                    executeUpdate();
                    next();
                    break;
                default:
                    Log.e(TAG, "Event not handled");
                    break;
            }
        }
    };
    
    private void executeUpdate() {
    	for (int i = 0; i < mCallback.size(); i++) {
    		try {
				mCallback.get(i).update();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    	}
    }
    
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SHOW_PROGRESS:
					int pos = (int) mLibVLC.getTime();
					if (mCallback.size() > 0) {
						executeUpdate();
						mHandler.removeMessages(SHOW_PROGRESS);
						sendEmptyMessageDelayed(SHOW_PROGRESS, 1000 - (pos % 1000));
					}
					break;
			}
		}
	};
    
    private void showNotification() {
		// add notification to status bar
		Notification notification = new Notification(R.drawable.icon, null,
		        System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		notificationIntent.putExtra(MainActivity.START_FROM_NOTIFICATION, "");
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, mCurrentMedia.getTitle(),
		        "## Artist ##", pendingIntent);
		startForeground(3, notification);
    }
    
    private void hideNotification() {
    	stopForeground(true);
    }
    
    
    
    private void pause() {
    	mHandler.removeMessages(SHOW_PROGRESS);
		// hideNotification(); <-- see event handler
		mLibVLC.pause();
    }
    
    private void play() {
    	mLibVLC.play();
		mHandler.sendEmptyMessage(SHOW_PROGRESS);
		showNotification();
    }
    
    private void stop() {
		mEventManager.removeHandler(mEventHandler);		
		mLibVLC.stop();
		mCurrentMedia = null;
		mMediaList.clear();
		mHandler.removeMessages(SHOW_PROGRESS);
		hideNotification();
		executeUpdate();
    }
    
    private void next() {
    	int index = mMediaList.indexOf(mCurrentMedia);
        if (index < (mMediaList.size() - 1)) {
        	mCurrentMedia = mMediaList.get(index + 1);
        	mLibVLC.readMedia(mCurrentMedia.getPath());
        } else {
        	stop();
        }
    }
    
    private IAudioService.Stub mInterface = new IAudioService.Stub() {
		
    	@Override
    	public String getCurrentMediaPath() throws RemoteException {
    		return mCurrentMedia.getPath();
    	}

    	@Override
    	public void pause() throws RemoteException {
    		AudioService.this.pause();
    	}

    	@Override
    	public void play() throws RemoteException {
    		AudioService.this.play();
    	}

    	@Override
    	public void stop() throws RemoteException {	
    		AudioService.this.stop();
    	}

    	@Override
    	public boolean isPlaying() throws RemoteException {
    		return mLibVLC.isPlaying();
    	}

		@Override
		public boolean hasMedia() throws RemoteException {
			return mMediaList.size() != 0;
		}

		@Override
		public String getArtist() throws RemoteException {
			// TODO: add media parameter
			return null;
		}

		@Override
		public String getTitle() throws RemoteException {
			if (mCurrentMedia != null)
				return mCurrentMedia.getTitle();
			else
				return null;
		}

		@Override
		public void addAudioCallback(IAudioServiceCallback cb)
				throws RemoteException {
			mCallback.add(cb);
			executeUpdate();
		}

		@Override
		public void removeAudioCallback(IAudioServiceCallback cb)
				throws RemoteException {
			if (mCallback.contains(cb)){
				mCallback.remove(cb);
			}
		}

		@Override
		public int getTime() throws RemoteException {
			return (int) mLibVLC.getTime();
		}

		@Override
		public int getLength() throws RemoteException {
			// TODO Auto-generated method stub
			return (int) mLibVLC.getLength();
		}

		@Override
		public void load(List<String> mediaPathList, int position) 
				throws RemoteException {
    		mEventManager.addHandler(mEventHandler); 
    		mMediaList.clear();
    		
    		DatabaseManager db = DatabaseManager.getInstance();
    		for (int i = 0; i < mediaPathList.size(); i++) {
    			String path = mediaPathList.get(i);
    			Media media = db.getMedia(path);
    			mMediaList.add(media);
    		}
    		
    		if (mMediaList.size() > position) {
    			mCurrentMedia = mMediaList.get(position);
    		}
    		
    		mLibVLC.readMedia(mCurrentMedia.getPath());
    		mHandler.sendEmptyMessage(SHOW_PROGRESS);
    		showNotification();
			
		}
	};

}