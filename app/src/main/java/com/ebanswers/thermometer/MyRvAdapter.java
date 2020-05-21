package com.ebanswers.thermometer;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MyRvAdapter extends RecyclerView.Adapter<MyRvAdapter.MyHoler> {

    private ArrayList<String> spList = new ArrayList<>();

    public MyRvAdapter(ArrayList<String> spList) {
        super();
        this.spList = spList;
    }

    @NonNull
    @Override
    public MyHoler onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sp_item,viewGroup,false);
        final MyHoler holder = new MyHoler(view);

        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.sMainActivity.tvSp.setText("串口："+spList.get(holder.getAdapterPosition()));
                MainActivity.sMainActivity.portPosition = holder.getAdapterPosition();
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyHoler myHoler, int i) {
        myHoler.textView.setText(spList.get(i));
    }

    @Override
    public int getItemCount() {
        return spList.size();
    }

    class MyHoler extends RecyclerView.ViewHolder {
        TextView textView;
        LinearLayout linearLayout;
        public MyHoler(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView_item);
            linearLayout = itemView.findViewById(R.id.layout_item);
        }
    }



}
