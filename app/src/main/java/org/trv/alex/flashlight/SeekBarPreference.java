package org.trv.alex.flashlight;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private static final int DEFAULT_VALUE = 0;

    private SeekBar mSeekBar;
    private TextView mValueTextView;
    private TextView mTitleTextView;
    private TextView mSummaryTextView;

    private int mProgress;
    private int mMax = 100;
    private String mValuePattern;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, 0, 0);

        mMax = ta.getInt(R.styleable.SeekBarPreference_max, mMax);
        mValuePattern = ta.getString(R.styleable.SeekBarPreference_value_pattern);

        ta.recycle();

        setLayoutResource(R.layout.seekbar_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSeekBar = view.findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mProgress);
        mSeekBar.setEnabled(isEnabled());

        mValueTextView = view.findViewById(R.id.seekbar_value);
        mValueTextView.setText(getFormattedString(mProgress));

        mTitleTextView = view.findViewById(R.id.seekbar_title);
        if (getTitle() == null) {
            mTitleTextView.setVisibility(View.GONE);
        } else {
            mTitleTextView.setText(getTitle());
        }

        mSummaryTextView = view.findViewById(R.id.seekbar_summary);
        if (getSummary() == null) {
            mSummaryTextView.setVisibility(View.GONE);
        } else {
            mSummaryTextView.setText(getSummary());
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setProgress(restorePersistedValue ? getPersistedInt(DEFAULT_VALUE) : (Integer) defaultValue);
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        if (mMax < progress) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        mProgress = progress;
        persistInt(mProgress);
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    private String getFormattedString(int progress) {
        if (mValuePattern == null) {
            return String.valueOf(progress);
        } else {
            return String.format(mValuePattern, progress);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mValueTextView.setText(getFormattedString(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (mProgress != progress) {
            setProgress(seekBar.getProgress());
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }

        SavedState savedState = new SavedState(superState);
        savedState.progress = mProgress;
        savedState.max = mMax;

        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        setMax(savedState.max);
        setProgress(savedState.progress);
    }

    private static class SavedState extends BaseSavedState {

        int max;
        int progress;

        public SavedState(Parcel source) {
            super(source);

            max = source.readInt();
            progress = source.readByte();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeInt(max);
            dest.writeInt(progress);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

    }

}
