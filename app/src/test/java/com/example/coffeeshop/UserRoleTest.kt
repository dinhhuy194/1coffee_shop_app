package com.example.coffeeshop

import com.example.coffeeshop.Model.User
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests cho RBAC Role system trên User model.
 * Đảm bảo đồng bộ với AuthContext.tsx trên admin dashboard.
 */
class UserRoleTest {

    // ─── Test Role Constants ────────────────────────────────────

    @Test
    fun `role constants match admin dashboard values`() {
        assertEquals("customer", User.ROLE_CUSTOMER)
        assertEquals("staff", User.ROLE_STAFF)
        assertEquals("admin", User.ROLE_ADMIN)
        assertEquals("superadmin", User.ROLE_SUPERADMIN)
    }

    // ─── Test Default Role ──────────────────────────────────────

    @Test
    fun `default user role is customer`() {
        val user = User(uid = "test-uid-001", name = "Test User", email = "test@coffee.com")
        assertEquals(User.ROLE_CUSTOMER, user.role)
    }

    @Test
    fun `default user is not admin`() {
        val user = User(uid = "test-uid-002")
        assertFalse(user.isAdmin())
    }

    @Test
    fun `default user is not superadmin`() {
        val user = User(uid = "test-uid-003")
        assertFalse(user.isSuperAdmin())
    }

    @Test
    fun `default user is not staff or above`() {
        val user = User(uid = "test-uid-004")
        assertFalse(user.isStaffOrAbove())
    }

    // ─── Test isAdmin() ─────────────────────────────────────────

    @Test
    fun `admin role returns isAdmin true`() {
        val user = User(uid = "admin-001", role = User.ROLE_ADMIN)
        assertTrue(user.isAdmin())
    }

    @Test
    fun `superadmin role returns isAdmin true`() {
        val user = User(uid = "sa-001", role = User.ROLE_SUPERADMIN)
        assertTrue(user.isAdmin())
    }

    @Test
    fun `staff role returns isAdmin false`() {
        val user = User(uid = "staff-001", role = User.ROLE_STAFF)
        assertFalse(user.isAdmin())
    }

    @Test
    fun `customer role returns isAdmin false`() {
        val user = User(uid = "cust-001", role = User.ROLE_CUSTOMER)
        assertFalse(user.isAdmin())
    }

    // ─── Test isSuperAdmin() ────────────────────────────────────

    @Test
    fun `superadmin role returns isSuperAdmin true`() {
        val user = User(uid = "sa-002", role = User.ROLE_SUPERADMIN)
        assertTrue(user.isSuperAdmin())
    }

    @Test
    fun `admin role returns isSuperAdmin false`() {
        val user = User(uid = "admin-002", role = User.ROLE_ADMIN)
        assertFalse(user.isSuperAdmin())
    }

    @Test
    fun `staff role returns isSuperAdmin false`() {
        val user = User(uid = "staff-002", role = User.ROLE_STAFF)
        assertFalse(user.isSuperAdmin())
    }

    // ─── Test isStaffOrAbove() ──────────────────────────────────

    @Test
    fun `staff returns isStaffOrAbove true`() {
        val user = User(uid = "staff-003", role = User.ROLE_STAFF)
        assertTrue(user.isStaffOrAbove())
    }

    @Test
    fun `admin returns isStaffOrAbove true`() {
        val user = User(uid = "admin-003", role = User.ROLE_ADMIN)
        assertTrue(user.isStaffOrAbove())
    }

    @Test
    fun `superadmin returns isStaffOrAbove true`() {
        val user = User(uid = "sa-003", role = User.ROLE_SUPERADMIN)
        assertTrue(user.isStaffOrAbove())
    }

    @Test
    fun `customer returns isStaffOrAbove false`() {
        val user = User(uid = "cust-003", role = User.ROLE_CUSTOMER)
        assertFalse(user.isStaffOrAbove())
    }

    // ─── Test Edge Cases ────────────────────────────────────────

    @Test
    fun `empty role string returns not admin`() {
        val user = User(uid = "edge-001", role = "")
        assertFalse(user.isAdmin())
        assertFalse(user.isSuperAdmin())
        assertFalse(user.isStaffOrAbove())
    }

    @Test
    fun `unknown role string returns not admin`() {
        val user = User(uid = "edge-002", role = "moderator")
        assertFalse(user.isAdmin())
        assertFalse(user.isSuperAdmin())
        assertFalse(user.isStaffOrAbove())
    }

    @Test
    fun `role is case sensitive`() {
        val user = User(uid = "edge-003", role = "Admin") // Capital A
        assertFalse(user.isAdmin()) // Phải khớp chính xác
    }

    // ─── Test Rank Constants (backward compatibility) ───────────

    @Test
    fun `rank constants are unchanged`() {
        assertEquals("Normal", User.RANK_NORMAL)
        assertEquals("Silver", User.RANK_SILVER)
        assertEquals("Gold", User.RANK_GOLD)
        assertEquals("Diamond", User.RANK_DIAMOND)
    }

    // ─── Test Copy with Role ────────────────────────────────────

    @Test
    fun `copy user with new role works correctly`() {
        val customer = User(uid = "copy-001", name = "Huy", role = User.ROLE_CUSTOMER)
        assertFalse(customer.isAdmin())

        val promoted = customer.copy(role = User.ROLE_ADMIN)
        assertTrue(promoted.isAdmin())
        assertEquals("Huy", promoted.name) // Other fields unchanged
    }

    // ─── Test Order Status Constants ────────────────────────────

    @Test
    fun `order status constants exist`() {
        assertEquals("pending", com.example.coffeeshop.Model.Order.STATUS_PENDING)
        assertEquals("preparing", com.example.coffeeshop.Model.Order.STATUS_PREPARING)
        assertEquals("ready", com.example.coffeeshop.Model.Order.STATUS_READY)
        assertEquals("completed", com.example.coffeeshop.Model.Order.STATUS_COMPLETED)
        assertEquals("cancelled", com.example.coffeeshop.Model.Order.STATUS_CANCELLED)
    }

    @Test
    fun `order status labels are Vietnamese`() {
        assertEquals("Chờ xử lý", com.example.coffeeshop.Model.Order.getStatusLabel("pending"))
        assertEquals("Đang pha chế", com.example.coffeeshop.Model.Order.getStatusLabel("preparing"))
        assertEquals("Sẵn sàng giao", com.example.coffeeshop.Model.Order.getStatusLabel("ready"))
        assertEquals("Đã giao", com.example.coffeeshop.Model.Order.getStatusLabel("completed"))
        assertEquals("Đã hủy", com.example.coffeeshop.Model.Order.getStatusLabel("cancelled"))
    }

    @Test
    fun `all order statuses list has 5 items`() {
        assertEquals(5, com.example.coffeeshop.Model.Order.ALL_STATUSES.size)
    }
}
