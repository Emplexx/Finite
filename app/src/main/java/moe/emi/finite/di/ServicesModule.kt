package moe.emi.finite.di

//@Module
//@InstallIn(SingletonComponent::class)
//object ServicesModule {
//
//	@Singleton
//	@Provides
//	fun provideMoshi(
//
//	): Moshi = makeMoshi()
//
//	@Singleton
//	@Provides
//	fun provideAtpClient(
//		moshi: Moshi
//	): ApiClient = makeAtpClient(moshi)
//
//	@Singleton
//	@Provides
//	fun provideWebService(
//		@ApplicationContext appContext: Context,
//		atpClient: AtpClient,
//	): WebService = WebService(appContext, atpClient)
//}