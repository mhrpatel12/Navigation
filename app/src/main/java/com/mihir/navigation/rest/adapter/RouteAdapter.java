package com.mihir.navigation.rest.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mihir.navigation.R;

import java.util.List;

/**
 * Created by Mihir on 06-07-2017.
 */

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<String> listRoute;
    private int rowLayout;
    private Context mContext;

    public RouteAdapter(List<String> listRoute, int rowLayout, Context context) {
        this.listRoute = listRoute;
        this.rowLayout = rowLayout;
        this.mContext = context;
    }

    @Override
    public RouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(rowLayout, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RouteAdapter.RouteViewHolder holder, final int position) {
        holder.txtRouteInstruction.setText(listRoute.get(position) + "");
    }

    @Override
    public int getItemCount() {
        return listRoute.size();
    }

    public class RouteViewHolder extends RecyclerView.ViewHolder {

        TextView txtRouteInstruction;

        public RouteViewHolder(View itemView) {
            super(itemView);
            txtRouteInstruction = (TextView) itemView.findViewById(R.id.txtRouteInstruction);
        }
    }
}
