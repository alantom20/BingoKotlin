package com.home.bingokotlin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_room.*
import kotlinx.android.synthetic.main.row_room.view.*
import java.util.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    companion object{
        val TAG =  MainActivity::class.java.simpleName
        val RC_SING_IN = 100
    }

    private lateinit var adapter: FirebaseRecyclerAdapter<GameRoom, RoomHolder>
    private var member: Member?  = null
    var avatarIds = intArrayOf(R.drawable.avatar_0,
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nickname.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.let {
                showNickDialog(it.uid, nickname.text.toString())
            }
        }
        group_avatars.visibility = View.GONE
        avatar.setOnClickListener {
            group_avatars.visibility = if(group_avatars.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        avatar_0.setOnClickListener(this)
        avatar_1.setOnClickListener(this)
        avatar_2.setOnClickListener(this)
        avatar_3.setOnClickListener(this)
        avatar_4.setOnClickListener(this)
        avatar_5.setOnClickListener(this)
        avatar_6.setOnClickListener(this)
        fab.setOnClickListener {
            val roomText = EditText(this)
            roomText.setText("Welcome")
            AlertDialog.Builder(this)
                    .setTitle("Your nickname")
                    .setMessage("Please enter a nickname")
                    .setView(roomText)
                    .setNeutralButton("Cancel",null)
                    .setPositiveButton("OK") { dialog, which ->
                        val roomTitle = roomText.text.toString()
                        val room = GameRoom(roomTitle,member)
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .push().setValue(room, object : DatabaseReference.CompletionListener{
                                    override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
                                        val roomId = ref.key
                                        FirebaseDatabase.getInstance().getReference("rooms")
                                                .child(roomId.toString())
                                                .child("id")
                                                .setValue(roomId)
                                        val bingo = Intent(this@MainActivity,BingoActivity::class.java)
                                        bingo.putExtra("ROOM_ID",roomId)
                                        bingo.putExtra("IC_CREATOR",true)
                                        startActivity(bingo)
                                    }

                                })

                    }.show()

        }
        //RecyclerView for Game Room
        val query = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .limitToLast(30)
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(this)
        val options = FirebaseRecyclerOptions.Builder<GameRoom>()
                .setQuery(query,GameRoom::class.java)
                .build()
        adapter = object : FirebaseRecyclerAdapter<GameRoom,RoomHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
                val view = layoutInflater.inflate(R.layout.row_room,parent,false)
                return RoomHolder(view)
            }

            override fun onBindViewHolder(holder: RoomHolder, position: Int, model: GameRoom) {
                holder.imageView.setImageResource(avatarIds[model.init!!.avatarId])
                holder.titleText.text = model.title
                holder.itemView.setOnClickListener {
                    val bingo = Intent(this@MainActivity,BingoActivity::class.java)
                    bingo.putExtra("ROOM_ID",model.id)
                    startActivity(bingo)
                }
            }
        }
        recycler.adapter = adapter

    }
    class RoomHolder(view : View) : RecyclerView.ViewHolder(view){
        var titleText = view.room_title
        var imageView =  view.room_image
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(this)
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
        adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_signout -> FirebaseAuth.getInstance().signOut()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        auth.currentUser?.also {
           Log.d(TAG, it.uid)
            FirebaseDatabase.getInstance().getReference("users")
                    .child(it.uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            member = snapshot.getValue(Member::class.java)
                            member?.nickname?.also { nick ->
                                nickname.setText(nick)
                            } ?: showNickDialog(it)

                            member?.let {
                                avatar.setImageResource(avatarIds[it.avatarId])
                            }

                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })
           it.displayName?.run {
               FirebaseDatabase.getInstance()
                       .getReference("users")
                       .child(it.uid)
                       .child("displayName")
                       .setValue(it.displayName)
                       .addOnCompleteListener { Log.d(TAG, "done") }
           }

            FirebaseDatabase.getInstance().getReference("users")
                    .child(it.uid)
                    .child("uid")
                    .setValue(it.uid)


              /* FirebaseDatabase.getInstance().getReference("users")
                       .child(it.uid)
                   .child("nickname")
                   .addListenerForSingleValueEvent(object : ValueEventListener {
                       override fun onDataChange(snapshot: DataSnapshot) {
                           snapshot.value?.also { nick ->
                               Log.d(TAG, "nickname: $nick")
                           } ?: showNickDialog(it)
                       }

                       override fun onCancelled(error: DatabaseError) {

                       }

                   })*/

       } ?: signUp()


        /* if(auth.currentUser == null){
            signUp()

        }else{
            Log.d(TAG, "onAuthStateChanged: ${auth.currentUser?.uid} ")
        }*/
    }


    private fun showNickDialog(uid: String, nick: String?) {
        val editText = EditText(this)
        editText.setText(nick)
        AlertDialog.Builder(this)
                .setTitle("Your nickname")
                .setMessage("Please enter a nickname")
                .setView(editText)
                .setPositiveButton("OK") { dialog, which ->
                    val nickname = editText.text.toString()
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(uid)
                            .child("nickname")
                            .setValue(nickname)
                }.show()

    }

    private fun showNickDialog(user: FirebaseUser) {
        var nick = user.displayName
        var uid = user.uid
        showNickDialog(uid, nick)

    }

    private fun signUp() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(
                                Arrays.asList(
                                        AuthUI.IdpConfig.EmailBuilder().build(),
                                        AuthUI.IdpConfig.GoogleBuilder().build()
                                ))
                        .setIsSmartLockEnabled(false)
                        .build(), RC_SING_IN
        )
    }

    override fun onClick(view: View?) {
        var selected = when(view!!.id){
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            R.id.avatar_5 -> 5
            R.id.avatar_6 -> 6
            else ->0
        }
        group_avatars.visibility = View.GONE
        FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child("avatarId")
                .setValue(selected)
    }
}