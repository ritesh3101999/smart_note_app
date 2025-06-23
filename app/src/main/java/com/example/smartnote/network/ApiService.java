package com.example.smartnote.network;

import com.example.smartnote.model.Folder;
import com.example.smartnote.model.Note;
import com.example.smartnote.model.User;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // Authentication
    @FormUrlEncoded
    @POST("api/users/login")
    Call<ResponseBody> login(
            @Field("username") String username,
            @Field("password") String password
    );

    // User Management
    @POST("api/users")
    Call<User> createUser(@Body User user, @Query("roleName") String roleName);

    @GET("api/users/{id}")
    Call<User> getUserById(@Path("id") Long id);

    // Note Operations
    @GET("api/notes")
    Call<List<Note>> getNotes();

    @POST("api/notes")
    Call<Note> createNote(@Body Note note);

    @PUT("api/notes/{id}")
    Call<Note> updateNote(@Path("id") Long id, @Body Note note);

    @DELETE("api/notes/{id}")
    Call<Void> deleteNote(@Path("id") Long id);

    @POST("api/notes/bookmark/{id}")
    Call<Note> toggleBookmark(@Path("id") Long id);

    // NEW: Get all bookmarked notes
    @GET("api/notes/bookmarked")
    Call<List<Note>> getBookmarkedNotes();

    // Folder Operations
    @GET("api/folders")
    Call<List<Folder>> getFolders();

    @POST("api/folders")
    @Headers("Content-Type: application/json; charset=UTF-8")
    Call<Folder> createFolder(@Body Folder folder);

    @DELETE("api/folders/{id}")
    Call<Void> deleteFolder(@Path("id") Long id);

    @GET("api/notes/folder/{folderId}") // CHANGED: Path updated
    Call<List<Note>> getNotesByFolder(@Path("folderId") Long folderId);

    // Legacy endpoint cleanup
    @DELETE("api/notes/{id}")
    Call<Void> permanentDeleteNote(@Path("id") int id);

    //fetch csrf token
    @GET("api/public/csrf")
    Call<Void> getCsrfToken();

    @GET("api/notes/{id}") // For fetching a single note with details/versions
    Call<Note> getNoteWithVersions(@Path("id") Long id);

    @GET("api/notes/search")
    Call<List<Note>> searchNotes(@Query("keyword") String keyword);
}
