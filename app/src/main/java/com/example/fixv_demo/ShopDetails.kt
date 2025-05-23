import com.google.gson.annotations.Expose
import com.google.gson.GsonBuilder

data class ShopDetails(
    @Expose val name: String,
    @Expose val address: String,
    @Expose val phoneNumber: String,
    @Expose val email: String
    // Add other fields as needed
)

val gson = GsonBuilder()
    .excludeFieldsWithoutExposeAnnotation()
    .create()

val shopDetails = ShopDetails("Shop Name", "Address", "Phone", "Email")
val shopDetailsJson = gson.toJson(shopDetails)