package com.ldihackos.tellvport.di

import com.ldihackos.tellvport.api.API
import com.ldihackos.tellvport.utils.Constants.BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Singleton
    @Provides
    fun providesRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
    }

    @Singleton
    @Provides
    fun providesBackendAPI(retrofitBuilder: Retrofit.Builder): API {
        return retrofitBuilder.build().create(API::class.java)
    }
//
//    @Singleton
//    @Provides
//    fun providesNotesAPI(retrofitBuilder: Retrofit.Builder, okHttpClient: OkHttpClient): NotesAPI {
//        return retrofitBuilder.client(okHttpClient)
//            .build().create(NotesAPI::class.java)
//    }
}