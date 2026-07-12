package com.posepilot.app.ui.gallery

import android.net.Uri
import com.posepilot.app.data.db.SessionRecordDao
import com.posepilot.app.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var galleryRepository: GalleryRepository
    private lateinit var sessionRecordDao: SessionRecordDao
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        galleryRepository = mock(GalleryRepository::class.java)
        sessionRecordDao = mock(SessionRecordDao::class.java)
        `when`(sessionRecordDao.getAllRecords()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadPhotosInitiallyEmpty() = runTest {
        `when`(galleryRepository.getPhotos()).thenReturn(emptyList())
        viewModel = GalleryViewModel(galleryRepository, sessionRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.photos.value.size)
    }

    @Test
    fun testLoadPhotosPopulatesList() = runTest {
        val mockUri1 = mock(Uri::class.java)
        val mockUri2 = mock(Uri::class.java)
        val mockList = listOf(mockUri1, mockUri2)

        `when`(galleryRepository.getPhotos()).thenReturn(mockList)
        viewModel = GalleryViewModel(galleryRepository, sessionRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.photos.value.size)
        assertEquals(mockUri1, viewModel.photos.value[0])
    }

    @Test
    fun testDeletePhotoRemovesFromList() = runTest {
        val mockUri1 = mock(Uri::class.java)
        val mockUri2 = mock(Uri::class.java)
        val mockList = listOf(mockUri1, mockUri2)

        `when`(galleryRepository.getPhotos()).thenReturn(mockList)
        `when`(galleryRepository.deletePhoto(mockUri1)).thenReturn(true)

        viewModel = GalleryViewModel(galleryRepository, sessionRecordDao)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.photos.value.size)

        viewModel.deletePhoto(mockUri1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.photos.value.size)
        assertEquals(mockUri2, viewModel.photos.value[0])
    }
}
