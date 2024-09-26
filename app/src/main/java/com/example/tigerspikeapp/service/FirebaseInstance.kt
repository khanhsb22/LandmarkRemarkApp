package com.example.tigerspikeapp.service

import com.example.tigerspikeapp.adapter.SearchAdapter
import com.example.tigerspikeapp.db.UserInfo
import com.example.tigerspikeapp.utils.AddResult
import com.example.tigerspikeapp.utils.GetResult
import com.example.tigerspikeapp.utils.LoginResult
import com.example.tigerspikeapp.utils.RegisterResult
import com.example.tigerspikeapp.utils.SearchResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

/**
 * Take care of data manipulation with Firebase db and send
 * response to MainActivity through interfaces
 */
class FirebaseInstance {
    interface ILogin {
        fun login(username: String, password: String,
                  loginResult: LoginResult)
    }
    interface IRegister {
        fun register(registerResult: RegisterResult)
        fun error(ex: Exception)
    }
    interface IAddNote {
        fun addResult(addResult: AddResult, latLng: LatLng,
                      content: String, address: String)
        fun error(ex: Exception)
    }
    interface IGetAllNotes {
        fun sendNoteInfo(address: String, content: String,
                         latLng: LatLng, username: String, getResult: GetResult)
    }
    interface IGetSingleNote {
        fun sendSingleInfo(address: String, getResult: GetResult,
                           generatedKey: String)
    }
    interface IDeleteNote {
        fun deleted(result: Boolean)
    }
    interface ISearch {
        fun result(searchResult: SearchResult,
                   searchList: ArrayList<SearchAdapter.SearchModel>)
    }

    private object Holder {
        val INSTANCE = FirebaseInstance()
    }

    companion object {
        @JvmStatic
        fun getInstance(): FirebaseInstance {
            return Holder.INSTANCE
        }
        private lateinit var dbReference: DatabaseReference
        private const val ROOT = "Users"
        private const val NOTES = "Notes"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val LAT = "lat"
        private const val LNG = "lng"
        private const val ADDRESS = "address"
        private const val CONTENT = "content"
    }

    private lateinit var iLogin: ILogin
    private lateinit var iRegist: IRegister
    private lateinit var iAddNote: IAddNote
    private lateinit var iGetAllNotes: IGetAllNotes
    private lateinit var iGetSingleNote: IGetSingleNote
    private lateinit var iDeleteNote: IDeleteNote
    private lateinit var iSearch: ISearch

    fun setISearch(iSearch: ISearch) {
        this.iSearch = iSearch
    }

    fun setIDeleteNote(iDeleteNote: IDeleteNote) {
        this.iDeleteNote = iDeleteNote
    }

    fun setIGetSingleNote(iGetSingleNote: IGetSingleNote) {
        this.iGetSingleNote = iGetSingleNote
    }

    fun setIGetAllNotes(iGetAllNotes: IGetAllNotes) {
        this.iGetAllNotes = iGetAllNotes
    }

    fun setIAddNote(iAddNote: IAddNote) {
        this.iAddNote = iAddNote
    }

    fun setILogin(iLogin: ILogin) {
        this.iLogin = iLogin
    }

    fun setIRegister(iRegist: IRegister) {
        this.iRegist = iRegist
    }

    private fun getDbReference(): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference()
    }

    /**
     * Search with username and note content
     * */
    fun search(searchText: String) {
        dbReference = getDbReference().child(ROOT)
        // Find with username
        var isUsername = false
        var count = 0
        val searchList = ArrayList<SearchAdapter.SearchModel>()
        dbReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (username in snapshot.children) {
                        count++
                        val key = username.key.toString()
                        // If search text keyword equal with username, add all notes to searchList
                        if (key == searchText) {
                            // Get all notes base on username
                            dbReference.child(key).child(NOTES)
                                .addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(p0: DataSnapshot) {
                                    if (p0.exists()) {
                                        isUsername = true
                                        var i = 0
                                        for (note in p0.children) {
                                            i++
                                            val generatedKey = note.key.toString()
                                            dbReference.child(key).child(NOTES).child(generatedKey)
                                                .addValueEventListener(object : ValueEventListener{
                                                    override fun onDataChange(p1: DataSnapshot) {
                                                        if (p1.exists()) {
                                                            val lat = p1.child(LAT).value
                                                            val lng = p1.child(LNG).value
                                                            val latLng = LatLng(lat as Double, lng as Double)
                                                            val strContent = p1.child(CONTENT).value.toString()
                                                            val model = SearchAdapter.SearchModel()
                                                            model.latLng = latLng
                                                            model.content = strContent
                                                            model.username = key
                                                            searchList.add(model)
                                                            if (p0.childrenCount.toInt() == i) {
                                                                iSearch.result(SearchResult.SUCCESS, searchList)
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                    }
                                                })

                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                            break
                        }
                        // If can't username by search text keyword.
                        // Find all notes base on search text, add all notes to searchList
                        if (snapshot.childrenCount.toInt() == count) {
                            if (!isUsername) {
                                if (snapshot.exists()) {
                                    var i = 0
                                    var find = false
                                    for (uname in snapshot.children) {
                                        i++
                                        val uKey = uname.key.toString()
                                        dbReference.child(uKey).child(NOTES)
                                            .addListenerForSingleValueEvent(object : ValueEventListener{
                                                override fun onDataChange(p0: DataSnapshot) {
                                                    if (p0.exists()) {
                                                        for (note in p0.children) {
                                                            val generatedKey = note.key.toString()
                                                            dbReference.child(uKey).child(NOTES)
                                                                .child(generatedKey)
                                                                .addValueEventListener(object :
                                                                    ValueEventListener {
                                                                    override fun onDataChange(
                                                                        p1: DataSnapshot
                                                                    ) {
                                                                        if (p1.exists()) {
                                                                            val lat = p1.child(LAT).value
                                                                            val lng = p1.child(LNG).value
                                                                            val latLng = LatLng(lat as Double, lng as Double)
                                                                            val strContent = p1.child(CONTENT).value.toString()
                                                                            // Compare content in db match with search text
                                                                            if (strContent.contains(searchText)) {
                                                                                find = true
                                                                                val model =
                                                                                    SearchAdapter.SearchModel()
                                                                                model.latLng =
                                                                                    latLng
                                                                                model.content =
                                                                                    strContent
                                                                                model.username = uKey
                                                                                searchList.add(model)
                                                                            }
                                                                            // End result
                                                                            if (i == snapshot.childrenCount.toInt()) {
                                                                                if (find) {
                                                                                    iSearch.result(
                                                                                        SearchResult.SUCCESS,
                                                                                        searchList
                                                                                    )
                                                                                } else {
                                                                                    iSearch.result(
                                                                                        SearchResult.EMPTY,
                                                                                        searchList
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }

                                                                    override fun onCancelled(error: DatabaseError) {
                                                                    }
                                                                })
                                                        }
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                }
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    /**
     * Delete a note
     * */
    fun deleteNote(username: String, noteKey: String) {
        dbReference = getDbReference().child(ROOT)
        dbReference.child(username).child(NOTES).child(noteKey).removeValue()
            .addOnSuccessListener {
                // Deletion successful
                iDeleteNote.deleted(true)
            }
            .addOnFailureListener { error ->
                // Handle deletion failure
            }
    }

    /**
     * Get info for a single note
     * */
    fun getSingleNoteInfo(username: String, latLng: LatLng) {
        dbReference = getDbReference().child(ROOT)
        dbReference.child(username).child(NOTES).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (note in snapshot.children) {
                    val generatedKey = note.key.toString()
                    dbReference.child(username).child(NOTES).child(generatedKey)
                        .addValueEventListener(object : ValueEventListener{
                            override fun onDataChange(p0: DataSnapshot) {
                                val lat = p0.child(LAT).value
                                val lng = p0.child(LNG).value
                                if (lat == latLng.latitude && lng == latLng.longitude) {
                                    val address = p0.child(ADDRESS).value.toString()
                                    iGetSingleNote.sendSingleInfo(address, GetResult.SINGLE_SUCCESS, generatedKey)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    /**
     * Get all notes for all users
     * */
    fun getAllNotes() {
        dbReference = getDbReference().child(ROOT)
        dbReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (user in snapshot.children) {
                    val username = user.key.toString()
                    dbReference.child(username).child(NOTES)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(p0: DataSnapshot) {
                            for (note in p0.children) {
                                val generatedKey = note.key.toString()
                                dbReference.child(username).child(NOTES).child(generatedKey)
                                    .addValueEventListener(object : ValueEventListener{
                                        override fun onDataChange(p1: DataSnapshot) {
                                            if (p1.exists()) {
                                                val address = p1.child(ADDRESS).value.toString()
                                                val content = p1.child(CONTENT).value.toString()
                                                val lat = p1.child(LAT).value
                                                val lng = p1.child(LNG).value
                                                val latLng = LatLng(lat as Double, lng as Double)
                                                iGetAllNotes.sendNoteInfo(
                                                    address,
                                                    content,
                                                    latLng,
                                                    username,
                                                    GetResult.SUCCESS
                                                )
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                        }
                                    })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    /**
     * Add 1 note for current user into Firebase db
     * */
    fun addNote(
        userInfo: UserInfo,
        content: String,
        latLng: LatLng,
        address: String
    ) {
        dbReference = getDbReference().child(ROOT)
        val map: HashMap<String, Any> = HashMap()
        map[LAT] = latLng.latitude
        map[LNG] = latLng.longitude
        map[ADDRESS] = address
        map[CONTENT] = content
        // Push generate key for 1 note
        var generatedKey = ""
        dbReference.child(userInfo.username).child(NOTES).push()
            .updateChildren(map).addOnSuccessListener(object : ValueEventListener,
                OnSuccessListener<Void> {
                override fun onDataChange(snapshot: DataSnapshot) {
                    generatedKey = snapshot.key.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                }

                override fun onSuccess(p0: Void?) {
                    dbReference.child(userInfo.username).child(NOTES).child(generatedKey)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(p0: DataSnapshot) {
                                iAddNote.addResult(AddResult.SUCCESS, latLng, content, address)
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                }
            })
    }

    /**
     * Register new account
     * */
    fun register(username: String, password: String, retypePassword: String) {
        if (password != retypePassword) {
            iRegist.register(RegisterResult.RETYPE_PASS_WRONG)
        } else {
            dbReference = getDbReference().child(ROOT)
            dbReference.child(username).addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        iRegist.register(RegisterResult.USERNAME_EXIST)
                    } else {
                        // Add new user in db
                        val map: HashMap<String, Any> = HashMap()
                        map[USERNAME] = username
                        map[PASSWORD] = password
                        dbReference.child(username).updateChildren(map).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                iRegist.register(RegisterResult.SUCCESS)
                            }
                        }.addOnFailureListener { ex -> iRegist.error(ex) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    fun login(username: String, password: String) {
        dbReference = getDbReference().child(ROOT)
        dbReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (item in snapshot.children) {
                    val key = item.key
                    if (username == key) {
                        found = true
                        dbReference.child(key).addValueEventListener(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists() && snapshot.child(PASSWORD).exists()) {
                                    val strPass = snapshot.child(PASSWORD).value.toString()
                                    if (strPass == password) {
                                        // Login success
                                        iLogin.login(username, password, LoginResult.SUCCESS)
                                    } else {
                                        // Wrong password
                                        iLogin.login(username, "", LoginResult.WRONG_PASS)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })
                    } else {
                        continue
                    }
                }
                if (!found) {
                    // Account doesn't exist
                    iLogin.login("", "", LoginResult.ACCOUNT_NOT_EXIST)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}