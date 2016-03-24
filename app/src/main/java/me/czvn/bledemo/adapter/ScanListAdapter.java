package me.czvn.bledemo.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import me.czvn.bledemo.R;
import me.czvn.bledemo.datas.ScanData;

/**
 * Created by andy on 2016/2/26.
 *
 */
public final class ScanListAdapter extends BaseAdapter{
    private List<ScanData> list;
    private Context mContext;

    public ScanListAdapter(List<ScanData> list,Context context) {
        this.list = list;
        this.mContext=context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView=new TextView(mContext);
        textView.setText(list.get(position).getDeviceName());
        return textView;
    }
}
