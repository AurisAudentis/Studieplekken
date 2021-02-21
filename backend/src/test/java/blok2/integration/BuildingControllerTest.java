package blok2.integration;

import blok2.model.Building;
import blok2.model.reservables.Location;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestExecutionListeners;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class BuildingControllerTest extends BaseIntegrationTest {

    @Test
    @WithUserDetails(value = "student1", userDetailsServiceBeanName = "testUserDetails")
    public void testGetAllBuildings() throws Exception {
        mockMvc.perform(get("/building")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)); // only approved location
    }

    @Test
    @WithUserDetails(value = "student1", userDetailsServiceBeanName = "testUserDetails")
    public void testGetOneBuilding() throws Exception {
        mockMvc.perform(get("/building/"+ testBuilding.getBuildingId())).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testBuilding))); // only approved location
    }

    @Test
    @WithUserDetails(value = "student1", userDetailsServiceBeanName = "testUserDetails")
    public void testGetInvalidBuilding() throws Exception {
        mockMvc.perform(get("/building/"+ "7256884")).andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "testUserDetails")
    public void testPostNewbuilding() throws Exception {
        Building building = new Building(-1, "New building", "Place place");

        mockMvc.perform(post("/building").contentType("application/json").with(csrf()).content(objectMapper.writeValueAsString(building))).andDo(print())
                .andExpect(status().isOk());

        Assert.assertEquals(2, buildingDao.getAllBuildings().size());
    }

    @Test
    @WithUserDetails(value = "student1", userDetailsServiceBeanName = "testUserDetails")
    public void testPostNewbuildingForbidden() throws Exception {
        Building building = new Building(-1, "New building", "Place place");

        mockMvc.perform(post("/building").contentType("application/json").with(csrf()).content(objectMapper.writeValueAsString(building))).andDo(print())
                .andExpect(status().isForbidden());

        Assert.assertEquals(1, buildingDao.getAllBuildings().size());
    }

    @Test
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "testUserDetails")
    public void testDeleteBuilding() throws Exception {
        mockMvc.perform(delete("/building/"+testBuilding.getBuildingId()).with(csrf()))
                .andDo(print()).andExpect(status().isOk());

        Assert.assertEquals(0, buildingDao.getAllBuildings().size());
    }

    @Test
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "testUserDetails")
    public void testDeleteBuildingForbidden() throws Exception {
        mockMvc.perform(delete("/building/"+testBuilding.getBuildingId()).with(csrf()))
                .andDo(print()).andExpect(status().isOk());

        Assert.assertEquals(0, buildingDao.getAllBuildings().size());
    }



    @Test
    @WithUserDetails(value = "student1", userDetailsServiceBeanName = "testUserDetails")
    public void testPutBuildingForbidden() throws Exception {
        String name = testBuilding.getName();
        testBuilding.setName("New Name");
        mockMvc.perform(put("/building/"+testBuilding.getBuildingId()).contentType("application/json").with(csrf()).content(objectMapper.writeValueAsString(testBuilding))).andDo(print())
                .andExpect(status().isForbidden());

        testBuilding.setName(name);
        Assert.assertEquals(testBuilding, buildingDao.getBuildingById(testBuilding.getBuildingId()));
    }

    @Test
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "testUserDetails")
    public void testPutBuilding() throws Exception {
        testBuilding.setName("New Name");
        mockMvc.perform(put("/building/"+testBuilding.getBuildingId()).contentType("application/json").with(csrf()).content(objectMapper.writeValueAsString(testBuilding))).andDo(print())
                .andExpect(status().isOk());

        Assert.assertEquals(testBuilding, buildingDao.getBuildingById(testBuilding.getBuildingId()));
    }
}
