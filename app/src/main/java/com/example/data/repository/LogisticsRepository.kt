package com.example.data.repository

import com.example.data.local.LogisticsDao
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

class LogisticsRepository(private val logisticsDao: LogisticsDao) {
    val allCustomers: Flow<List<Customer>> = logisticsDao.getAllCustomers()
    val allDrivers: Flow<List<Driver>> = logisticsDao.getAllDrivers()
    val allBookings: Flow<List<Booking>> = logisticsDao.getAllBookings()
    val allUsers: Flow<List<User>> = logisticsDao.getAllUsers()
    val allPaymentTransactions: Flow<List<PaymentTransaction>> = logisticsDao.getAllPaymentTransactions()

    suspend fun getCustomerById(id: Int): Customer? = logisticsDao.getCustomerById(id)
    suspend fun insertCustomer(customer: Customer): Long = logisticsDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = logisticsDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = logisticsDao.deleteCustomer(customer)

    suspend fun getDriverById(id: Int): Driver? = logisticsDao.getDriverById(id)
    suspend fun insertDriver(driver: Driver): Long = logisticsDao.insertDriver(driver)
    suspend fun updateDriver(driver: Driver) = logisticsDao.updateDriver(driver)
    suspend fun deleteDriver(driver: Driver) = logisticsDao.deleteDriver(driver)

    suspend fun getBookingById(id: Int): Booking? = logisticsDao.getBookingById(id)
    suspend fun insertBooking(booking: Booking): Long = logisticsDao.insertBooking(booking)
    suspend fun updateBooking(booking: Booking) = logisticsDao.updateBooking(booking)
    suspend fun deleteBooking(booking: Booking) = logisticsDao.deleteBooking(booking)

    fun getDocumentsForBooking(bookingId: Int): Flow<List<Document>> = logisticsDao.getDocumentsForBooking(bookingId)
    suspend fun insertDocument(document: Document): Long = logisticsDao.insertDocument(document)
    suspend fun deleteDocument(document: Document) = logisticsDao.deleteDocument(document)

    suspend fun getUserByUsername(username: String): User? = logisticsDao.getUserByUsername(username)
    suspend fun insertUser(user: User): Long = logisticsDao.insertUser(user)

    fun getPaymentsForBooking(bookingId: Int): Flow<List<PaymentTransaction>> = logisticsDao.getPaymentsForBooking(bookingId)
    suspend fun insertPaymentTransaction(paymentTransaction: PaymentTransaction): Long = logisticsDao.insertPaymentTransaction(paymentTransaction)
    suspend fun deletePaymentTransaction(paymentTransaction: PaymentTransaction) = logisticsDao.deletePaymentTransaction(paymentTransaction)
}
