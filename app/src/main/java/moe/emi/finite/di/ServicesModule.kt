package moe.emi.finite.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import moe.emi.finite.service.notifications.AlarmScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServicesModule {
	@Singleton
	@Provides
	fun provideAlarmScheduler(@ApplicationContext c: Context): AlarmScheduler = AlarmScheduler(c)
}