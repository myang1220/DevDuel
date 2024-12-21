import { useEffect, useState } from "react";
import UserBlock from "./UserBlock";
import { queryAPI } from "../../api";

interface User {
  displayName: string;
  userID: string;
  email: string;
}

// shows all users ever in side panel
export default function OnlineUsers() {
  const [inGameUsers, setInGameUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  // get a list of all users using UserList endpoint
  const fetchUsers = async () => {
    try {
      const response = await queryAPI("UserList");
      if (response.response_type === "success") {
        const users = response.data as User[];
        setInGameUsers(users);
      } else {
        console.error("Failed to fetch users:", response.error);
      }
    } catch (error) {
      console.error("Error fetching users:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // sort users so that guests goes at the end
  function sortUsers(a: User, b: User) {
    const isAGuest = a.displayName.toLowerCase().startsWith("guest");
    const isBGuest = b.displayName.toLowerCase().startsWith("guest");

    if (isAGuest && !isBGuest) {
      return 1; // a should come after b
    } else if (!isAGuest && isBGuest) {
      return -1; // a should come before b
    } else {
      return a.displayName.localeCompare(b.displayName); // sort alphabetically if both are guests or both are not guests
    }
  }

  return (
    <div className="w-full h-full flex flex-col items-center justify-center bg-black overflow-hidden p-1 z-10 border-l-4 border-sky-200 border-opacity-70">
      <h1 className="relative h-1/8 flex justify-center items-center text-gray-200 font-bold text-3xl font-courier py-4">
        <div className="absolute top-6 left-4 w-full h-1/2 bg-sky-800 opacity-75 rounded-lg"></div>
        <span className="relative z-10 shadow-lg">All Users</span>
      </h1>
      <div className="h-4/5 overflow-y-scroll p-2 scrollable-content">
        {loading ? (
          <p className="text-gray-400">Loading In Game Users...</p>
        ) : (
          <div className="flex flex-wrap justify-center gap-1">
            {inGameUsers.length > 0 ? (
              inGameUsers
                .sort(sortUsers)
                .map((user) => (
                  <UserBlock key={user.userID} name={user.displayName} />
                ))
            ) : (
              <p className="text-gray-400">No users in game currently.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
