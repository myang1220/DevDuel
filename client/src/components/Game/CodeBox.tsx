import React, { useEffect, useState } from "react";
import CodeEditor from "./components/CodeEditor.tsx";
import Problem from "./components/Problem.tsx";
import Score from "./components/Score.tsx";
import Output from "./components/Output.tsx";
import Header from "../Header.tsx";
import ResultPage from "./ResultPage.tsx";
import { queryAPI } from "../../api.ts";
import { useGlobalDispatch, useGlobalState } from "./GlobalStateProvider.tsx";
import { useUser } from "@clerk/clerk-react";

// manages game page design and logic
export default function Codebox(roomID: any) {
  const [leftWidth, setLeftWidth] = useState<number>(40);
  const [rightWidth, setRightWidth] = useState<number>(60);
  const [topLeftHeight, setTopLeftHeight] = useState<number>(70);
  const [topHeight, setTopHeight] = useState<number>(60);
  const [isFinished, setIsFinished] = useState<boolean>(false);
  const [winner, setWinner] = useState<boolean>(false);
  const [timeTaken, setTimeTaken] = useState<string>("");
  const [problemID, setProblemID] = useState<string | null>(null);
  const [duration, setDuration] = useState<number>(60);
  const [startTime, setStartTime] = useState<Date | null>(null);
  const [remainingTime, setRemainingTime] = useState<number>(60);
  const [pageTitle, setPageTitle] = useState<string>("");
  const user = useUser().user;
  let username = user?.username
    ? user.username
    : user?.emailAddresses
        ?.find((email) => email.id === user.primaryEmailAddressId)
        ?.emailAddress?.split("@")[0];
  if (!username) {
    username = localStorage.getItem("guestName") || "";
  }
  const dispatch = useGlobalDispatch();
  const state = useGlobalState();

  // fetch room info using RoomInfo endpoint
  async function getRoomInfo() {
    try {
      dispatch({ type: "SET_ROOMID", payload: roomID.roomID });
      const response = await queryAPI("RoomInfo", { roomID: roomID.roomID });
      if (response.response_type === "success") {
        console.log("Room info loaded:", response.data);
        setProblemID(response.data.problemID);
        const { duration } = response.data;
        setDuration(duration);
        setRemainingTime(duration);
      } else {
        console.error("Failed to fetch room info:", response);
      }
    } catch (error) {
      console.error("Error connecting to backend:", error);
    }
  }

  useEffect(() => {
    if (!isFinished) {
      getRoomInfo();
    }
  }, []);

  // initialize start time using local storage if first load
  useEffect(() => {
    const storedStartTime = localStorage.getItem("startTime");

    if (storedStartTime) {
      const parsedStartTime = new Date(storedStartTime);
      if (!isNaN(parsedStartTime.getTime())) {
        setStartTime(parsedStartTime);
      } else {
        console.error("Invalid startTime in localStorage. Resetting...");
        const newStartTime = new Date();
        setStartTime(newStartTime);
        localStorage.setItem("startTime", newStartTime.toISOString());
      }
    } else {
      const newStartTime = new Date();
      setStartTime(newStartTime);
      localStorage.setItem("startTime", newStartTime.toISOString());
    }
  }, []);

  // update remaining time every second
  useEffect(() => {
    if (!startTime || isFinished) return;

    async function getWinner() {
      const strFracToNum = (score: string) => {
        const parts = score.split("/");
        return parseInt(parts[0], 10) / parseInt(parts[1], 10);
      };

      try {
        const response = await queryAPI("RoomInfo", { roomID: roomID.roomID });
        if (response.response_type === "success") {
          const playerData = Object.entries(response.data.players).map(
            ([key, value]: [string, any]) => {
              return {
                key,
                userName: value.displayName,
                userScore: value.userScore,
                timeSubmitted: value.timeSubmitted,
              };
            }
          );
          // check if the players have the same score and determine the winner
          if (playerData[0].userName === username) {
            if (
              strFracToNum(playerData[0].userScore) >
              strFracToNum(playerData[1].userScore)
            ) {
              onFinish(true);
            } else if (
              strFracToNum(playerData[0].userScore) <
              strFracToNum(playerData[1].userScore)
            ) {
              onFinish(false);
            } else {
              if (
                strFracToNum(playerData[0].userScore) === 0 &&
                strFracToNum(playerData[1].userScore) === 0
              ) {
                onFinish(false);
              } else {
                onFinish(
                  new Date(playerData[0].timeSubmitted).getTime() -
                    new Date(response.data.timeCreated).getTime() <
                    new Date(playerData[1].timeSubmitted).getTime() -
                      new Date(response.data.timeCreated).getTime()
                );
              }
            }
          } else {
            if (
              strFracToNum(playerData[0].userScore) >
              strFracToNum(playerData[1].userScore)
            ) {
              onFinish(false);
            } else if (
              strFracToNum(playerData[0].userScore) <
              strFracToNum(playerData[1].userScore)
            ) {
              onFinish(true);
            } else {
              if (
                strFracToNum(playerData[0].userScore) === 0 &&
                strFracToNum(playerData[1].userScore) === 0
              ) {
                onFinish(false);
              } else {
                onFinish(
                  new Date(playerData[0].timeSubmitted).getTime() -
                    new Date(response.data.timeCreated).getTime() >
                    new Date(playerData[1].timeSubmitted).getTime() -
                      new Date(response.data.timeCreated).getTime()
                );
              }
            }
          }
        } else {
          console.error("Failed to fetch room info:", response);
        }
      } catch (error) {
        console.error("Error connecting to backend:", error);
      }
    }

    const interval = setInterval(() => {
      const elapsedTime = Math.floor((Date.now() - startTime.getTime()) / 1000);
      const userRemainingTime = Math.max(0, duration - elapsedTime);
      setRemainingTime(userRemainingTime);

      // only call getWinner if at the end of the game
      if (userRemainingTime === 0) {
        getWinner();
        clearInterval(interval);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [startTime, duration]);

  // update page title with remaining time
  useEffect(() => {
    if (remainingTime >= 0 && !isFinished) {
      setPageTitle(formatTime(remainingTime));
    }
  }, [remainingTime]);

  // helper function to format time in MM:SS
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, "0")}`;
  };

  // on user finish
  function onFinish(winner: boolean) {
    if (!state.code || state.code.trim() === "") {
      const problemSignature = state.problem?.signature[state.language] || "";
      dispatch({ type: "SET_CODE", payload: problemSignature });
    }
    if (!isFinished) {
      setWinner(winner);
      setIsFinished(true);
    }
  }

  // set time taken in result page on game finish
  useEffect(() => {
    if (isFinished) {
      setTimeTaken(formatTime(duration - remainingTime));
    }
  }, [isFinished]);

  // splitter logic
  const handleMouseDown = (
    e: React.MouseEvent<HTMLDivElement>,
    direction: "vertical" | "horizontal-left" | "horizontal-right"
  ) => {
    e.preventDefault();

    const startX = e.clientX;
    const startY = e.clientY;
    const initialLeftWidth = leftWidth;
    const initialTopHeight = topHeight;
    const initialTopLeftHeight = topLeftHeight;

    const onMouseMove = (e: MouseEvent) => {
      if (direction === "vertical") {
        const deltaX = e.clientX - startX;
        const newLeftWidth = Math.max(
          30,
          Math.min(70, initialLeftWidth + (deltaX / window.innerWidth) * 100)
        );
        setLeftWidth(newLeftWidth);
        setRightWidth(100 - newLeftWidth);
      } else if (direction === "horizontal-right") {
        const deltaY = e.clientY - startY;
        const newTopHeight = Math.max(
          30,
          Math.min(80, initialTopHeight + (deltaY / window.innerHeight) * 100)
        );
        setTopHeight(newTopHeight);
      } else if (direction === "horizontal-left") {
        const deltaY = e.clientY - startY;
        const newTopLeftHeight = Math.max(
          30,
          Math.min(
            70,
            initialTopLeftHeight + (deltaY / window.innerHeight) * 100
          )
        );
        setTopLeftHeight(newTopLeftHeight);
      }
    };

    const onMouseUp = () => {
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);
    };

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  };

  return (
    <div className="flex flex-col w-full h-full overflow-hidden">
      {isFinished && problemID && (
        <ResultPage
          winner={winner}
          timeTaken={timeTaken}
          problemID={problemID}
          userID={user?.id || username || ""}
        />
      )}
      {/* Static Header */}
      <div className="w-full h-[15%]">
        <Header pageTitle={pageTitle} />
      </div>

      {/* Main Layout */}
      <div className="flex flex-1 h-[85%] overflow-hidden">
        {/* Left Panels */}
        <div
          className="flex flex-col overflow-hidden"
          style={{ width: `${leftWidth}%` }}
        >
          <div
            className="flex items-center justify-center overflow-auto"
            style={{ height: `${topLeftHeight}%` }}
          >
            <div className="font-montserrat w-full h-full p-2">
              <div className="w-full h-full px-4 py-2 bg-[#1E1E1E] rounded-lg overflow-auto">
                {problemID ? (
                  <Problem problemID={problemID} />
                ) : (
                  <p>Loading problem...</p>
                )}
              </div>
            </div>
          </div>
          {/* Horizontal Resizer */}
          <div
            className="z-10 h-[5px] cursor-row-resize bg-gray-500"
            onMouseDown={(e) => handleMouseDown(e, "horizontal-left")}
          />
          <div
            className="flex items-center justify-center overflow-auto"
            style={{ height: `${100 - topLeftHeight}%` }}
          >
            <div className="font-montserrat w-full h-full p-2">
              <div className="w-full h-full px-4 py-2 bg-[#1E1E1E] rounded-lg overflow-auto">
                <Score onFinish={onFinish} />
              </div>
            </div>
          </div>
        </div>

        {/* Vertical Resizer */}
        <div
          className="z-10 bg-gray w-[5px] cursor-col-resize bg-gray-500"
          onMouseDown={(e) => handleMouseDown(e, "vertical")}
        />

        {/* Right Panels */}
        <div
          className="flex flex-col overflow-hidden"
          style={{ width: `${rightWidth}%` }}
        >
          <div
            className="flex items-center justify-center overflow-auto"
            style={{ height: `${topHeight}%` }}
          >
            <div className="font-montserrat w-full h-full p-2">
              <div className="w-full h-full bg-[#1E1E1E] rounded-lg overflow-auto">
                <CodeEditor onFinish={onFinish} />
              </div>
            </div>
          </div>
          <div
            className="z-10 h-[5px] cursor-row-resize bg-gray-500"
            onMouseDown={(e) => handleMouseDown(e, "horizontal-right")}
          />
          <div
            className="flex items-center justify-center overflow-auto"
            style={{ height: `${100 - topHeight}%` }}
          >
            <div className="font-montserrat w-full h-full p-2">
              <div className="w-full h-full bg-[#1E1E1E] rounded-lg overflow-auto">
                <Output />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
