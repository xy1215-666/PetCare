package io.petcare.backend.health;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("health_record")
public class HealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("pet_id")
    private Long petId;
    @TableField("record_date")
    private LocalDate recordDate;
    private AppetiteLevel appetite;
    @TableField("water_intake")
    private WaterIntake waterIntake;
    @TableField("energy_level")
    private EnergyLevel energyLevel;
    @TableField("activity_level")
    private ActivityLevel activityLevel;
    @TableField("sleep_status")
    private SleepStatus sleepStatus;
    @TableField("stool_status")
    private StoolStatus stoolStatus;
    @TableField("urination_status")
    private UrinationStatus urinationStatus;
    private Boolean abnormal;
    @TableField("abnormal_description")
    private String abnormalDescription;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public AppetiteLevel getAppetite() { return appetite; }
    public void setAppetite(AppetiteLevel appetite) { this.appetite = appetite; }
    public WaterIntake getWaterIntake() { return waterIntake; }
    public void setWaterIntake(WaterIntake waterIntake) { this.waterIntake = waterIntake; }
    public EnergyLevel getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(EnergyLevel energyLevel) { this.energyLevel = energyLevel; }
    public ActivityLevel getActivityLevel() { return activityLevel; }
    public void setActivityLevel(ActivityLevel activityLevel) { this.activityLevel = activityLevel; }
    public SleepStatus getSleepStatus() { return sleepStatus; }
    public void setSleepStatus(SleepStatus sleepStatus) { this.sleepStatus = sleepStatus; }
    public StoolStatus getStoolStatus() { return stoolStatus; }
    public void setStoolStatus(StoolStatus stoolStatus) { this.stoolStatus = stoolStatus; }
    public UrinationStatus getUrinationStatus() { return urinationStatus; }
    public void setUrinationStatus(UrinationStatus urinationStatus) { this.urinationStatus = urinationStatus; }
    public Boolean getAbnormal() { return abnormal; }
    public void setAbnormal(Boolean abnormal) { this.abnormal = abnormal; }
    public String getAbnormalDescription() { return abnormalDescription; }
    public void setAbnormalDescription(String abnormalDescription) { this.abnormalDescription = abnormalDescription; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
