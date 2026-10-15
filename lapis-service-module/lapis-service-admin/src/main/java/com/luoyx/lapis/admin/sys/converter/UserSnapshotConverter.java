package com.luoyx.lapis.admin.sys.converter;

import com.luoyx.lapis.admin.sys.entity.UserProfile;
import com.luoyx.lapis.admin.sys.entity.UserSnapshot;
import com.luoyx.lapis.usersnapshot.msg.UserSnapshotMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserSnapshotConverter {

    UserSnapshot toUserSnapshot(UserProfile userProfile);

    UserSnapshotMessage.UserSnapshot toMsgUserSnapshot(UserSnapshot userSnapshot);

    List<UserSnapshotMessage.UserSnapshot> toMsgUserSnapshotList(List<UserSnapshot> userSnapshotList);
}
