import { useEffect, useState } from "react";
import { useUser } from "@clerk/clerk-react";
import { queryAPI } from "../../api";

interface User {
  displayName: string;
  userID: string;
  wins: string;
  email: string;
  startDate: string;
}

// shows user stats and leaderboard
export default function Stats() {
  const { user } = useUser();

  const [userStats, setUserStats] = useState<{
    displayName: string;
    wins: number;
    email: string;
    startDate: string;
    userID: string;
  } | null>(null);
  const [leaderboard, setLeaderboard] = useState<User[]>([]);
  const [loadingLeaderboard, setLoadingLeaderboard] = useState(true);
  const [loadingStats, setLoadingStats] = useState(true);

  // use UserLeaderboard endpoint to get leaderboard
  const fetchLeaderboard = async () => {
    try {
      const response = await queryAPI("UserLeaderboard");
      if (response.response_type === "success") {
        setLeaderboard(response.data as User[]);
        console.log("Leaderboard loaded:", response.data);
      } else {
        console.error("Failed to fetch leaderboard:", response);
      }
    } catch (error) {
      console.error("Error fetching leaderboard:", error);
    } finally {
      setLoadingLeaderboard(false);
    }
  };

  // use UserInfo endpoint to get user stats
  const fetchUserStats = async () => {
    if (!user) return;

    try {
      const response = await queryAPI("UserInfo", { userID: user.id });
      if (response.response_type === "success") {
        setUserStats(response.data);
        console.log("User stats fetched successfully:", response.data);
      } else {
        console.error("Failed to fetch user stats:", response);
      }
    } catch (error) {
      console.error("Error fetching user stats:", error);
    } finally {
      setLoadingStats(false);
    }
  };

  useEffect(() => {
    fetchLeaderboard();
    fetchUserStats();
  }, []);

  // Loading state for leaderboard
  if (loadingLeaderboard || loadingStats) {
    return (
      <div className="w-full h-full flex justify-center items-center">
        <p className="text-gray-400 text-lg">Loading stats...</p>
      </div>
    );
  }

  // Handle case where no leaderboard data is available
  if (!leaderboard.length) {
    return (
      <div className="w-full h-full flex justify-center items-center">
        <p className="text-gray-400 text-lg">No leaderboard data available.</p>
      </div>
    );
  }

  const { displayName, wins, email, startDate } = userStats || {
    displayName: "Unknown",
    wins: 0,
    email: "Unknown",
    startDate: "Unknown",
  };

  return (
    <div className="w-full h-full flex flex-col bg-black overflow-hidden p-1 z-10 border-l-4 border-sky-200 border-opacity-70">
      <div className="h-1/2 bg-black rounded-lg shadow-lg">
        <h1 className="relative h-1/5 flex justify-center items-center text-gray-200 font-bold text-3xl font-courier py-4">
          <div className="absolute inset-0 w-[57%] h-1/2 translate-x-24 translate-y-6 bg-sky-800 opacity-75 rounded-lg"></div>
          <span className="relative z-10">Leaderboard</span>
        </h1>
        <div className="h-4/5 overflow-y-scroll p-2 scrollable-content">
          <div className="flex flex-col justify-start pl-14 pr-6 gap-1 font-montserrat text-gray-500 break-words">
            {leaderboard.map((user, index) => {
              const isCurrentUser = user.userID === userStats?.userID;
              return (
                <p
                  key={user.userID}
                  className={`p-1 rounded ${isCurrentUser
                    ? "bg-sky-700 text-white font-bold"
                    : "bg-transparent text-gray-400"
                    }`}
                >
                  <b className="font-bold">
                    #{index + 1})
                  </b>{" "}
                  {user.displayName} -{" "}
                  <span className="text-yellow-600">{user.wins} wins</span>
                </p>
              );
            })}
          </div>
        </div>
      </div>
      <div className="h-1/2 bg-black rounded-lg shadow-lg">
        <h1 className="relative h-1/5 flex justify-center items-center text-gray-200 font-bold text-3xl font-courier py-4">
          <div className="absolute inset-0 w-[27%] h-1/2 translate-x-36 translate-y-6 bg-sky-800 opacity-75 rounded-lg"></div>
          <span className="relative z-10">Stats</span>
        </h1>
        <div className="h-4/5 overflow-y-scroll p-2 scrollable-content">
          <div className="flex flex-col justify-start pl-14 pr-6 gap-2 font-montserrat text-gray-500 break-words">
            <p>
              <b className="font-bold text-gray-400">Username: </b>
              {displayName}
            </p>
            <p>
              <b className="font-bold text-gray-400">Email: </b>
              {email}
            </p>
            <p>
              <b className="font-bold text-gray-400">Leaderboard Rank: </b>
              {leaderboard.findIndex((user) => user.userID === userStats?.userID) + 1}
            </p>
            <p>
              <b className="font-bold text-gray-400">Total Wins: </b>
              {wins}
            </p>
            <p>
              <b className="font-bold text-gray-400">Account Created: </b>
              {startDate}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

