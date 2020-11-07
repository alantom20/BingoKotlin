package com.home.bingokotlin

import android.app.AlertDialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_bingo.*
import kotlinx.android.synthetic.main.single_button.view.*

class BingoActivity : AppCompatActivity(), View.OnClickListener {
    companion object{
        val TAG = BingoActivity::class.java.simpleName
        val STATUS_INIT = 0
        val STATUS_CREATED = 1
        val STATUS_JOINED = 2
        val STATUS_CREATOR_TURN  = 3
        val STATUS_JOINER_TURN = 4
        val STATUS_CREATOR_BINGO = 5
        val STATUS_JOINER_BINGO = 6
    }

    private lateinit var adapter: FirebaseRecyclerAdapter<Boolean, NumberHolder>
    private var isCreator: Boolean = false
    private lateinit var roomId: String
    var myTurn:Boolean = false
    set(value) {
        field = value
        info.setText(if (value) "請選擇號碼" else "等待對手選號")
    }
    val statusListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            var status : Long = snapshot.getValue() as Long
            when(status.toInt()){
                STATUS_CREATED -> {
                    info.setText("等待對手加入")
                }
                STATUS_JOINED ->{
                    info.setText("YA!對手加入了")
                    FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId)
                            .child("status")
                            .setValue(STATUS_CREATOR_TURN)
                }
                STATUS_CREATOR_TURN ->{
                    myTurn = isCreator

                }
                STATUS_JOINER_TURN ->{
                    myTurn = !isCreator
                }
                STATUS_CREATOR_BINGO ->{
                    AlertDialog.Builder(this@BingoActivity)
                            .setTitle("Bingo")
                            .setMessage(if (isCreator) "恭喜你,賓果了!" else "對方賓果了!")
                            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                            endGame()
                            }).show()
                }
                STATUS_JOINER_BINGO ->{
                    AlertDialog.Builder(this@BingoActivity)
                            .setTitle("Bingo")
                            .setMessage(if (!isCreator) "恭喜你,賓果了!" else "對方賓果了!")
                            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                                endGame()
                            }).show()
                }


            }
        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    private fun endGame() {
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .removeEventListener(statusListener)
        if (isCreator){
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .removeValue()
        }
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bingo)
        roomId = intent.getStringExtra("ROOM_ID")
        isCreator = intent.getBooleanExtra("IC_CREATOR",false)
        Log.d(TAG, "roomId :　$roomId")
        if(isCreator) {
            for (i in 1..25) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomId)
                        .child("number")
                        .child(i.toString())
                        .setValue(false)
            }

            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(STATUS_CREATED)
        }else{
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(STATUS_JOINED)
        }
        //map for number for position
        val numberMap = HashMap<Int,Int>()
        val buttons = mutableListOf<NumberButton>()
        for(i in 0..24 ){
            val button = NumberButton(this)
            button.number = i+1
            buttons.add(button)
        }
        buttons.shuffle()
        for(i in 0..24){
            numberMap.put(buttons.get(i).number,i)
        }
        recycler_bingo.setHasFixedSize(true)
        recycler_bingo.layoutManager = GridLayoutManager(this,5)
        //Adapter
        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId)
            .child("number")
            .orderByKey()
        val option = FirebaseRecyclerOptions.Builder<Boolean>()
            .setQuery(query,Boolean::class.java)
            .build()

        adapter = object : FirebaseRecyclerAdapter<Boolean,NumberHolder>(option){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.single_button,parent,false)
                return  NumberHolder(view)
            }

            override fun onBindViewHolder(holder: NumberHolder, position: Int, model: Boolean) {
                holder.button.setText(buttons.get(position).number.toString())
                holder.button.number = buttons.get(position).number
                holder.button.isEnabled = !buttons.get(position).picked
                holder.button.setOnClickListener(this@BingoActivity)
            }

            override fun onChildChanged(type: ChangeEventType,
                                        snapshot: DataSnapshot,
                                        newIndex: Int,
                                        oldIndex: Int)
            {
                super.onChildChanged(type, snapshot, newIndex, oldIndex)
                Log.d(TAG, "onChildChanged:$type / $snapshot / ${snapshot.key} / ${snapshot.getValue()}")
                if (type == ChangeEventType.CHANGED){
                    var number : Int? = snapshot.key?.toInt()
                    var picked : Boolean = snapshot.getValue() as Boolean
                    var pos : Int? = numberMap.get(number)
                    buttons.get(pos!!).picked =  picked
                    var holder : NumberHolder = recycler_bingo.findViewHolderForAdapterPosition(pos) as NumberHolder
                    holder.button.isEnabled = !picked

                    //counting Bingo
                    val nums = IntArray(25)

                    for(i in 0..24){
                        nums[i] = if(buttons.get(i).picked) 1 else 0
                    }
                    var bingo = 0
                    for(i in 0..4){
                        var sum = 0
                        for (j in 0..4){
                            sum +=nums[i*5+j]
                        }
                        bingo += if (sum==5) 1 else 0
                        sum = 0
                            for (j in 0..4){
                                sum +=nums[j*5+i]
                    }
                            bingo += if (sum==5) 1 else 0

                        Log.d(TAG, "onChildChanged: $bingo")
                        if(bingo > 0){
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("status")
                                    .setValue(if(isCreator) STATUS_CREATOR_BINGO else STATUS_JOINER_BINGO)
                        }

                    }
                }
            }
        }
        recycler_bingo.adapter = adapter

    }
    class NumberHolder(view : View) : RecyclerView.ViewHolder(view) {
       var button : NumberButton = view.button

    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .addValueEventListener(statusListener)
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("status")
                .removeEventListener(statusListener)
    }

    override fun onClick(v: View?) {
        if(myTurn){
            val number = (v as NumberButton).number
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("number")
                    .child(number.toString())
                    .setValue(true)
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .child("status")
                    .setValue(if (isCreator) STATUS_JOINER_TURN else STATUS_CREATOR_TURN)
        }

    }


}
