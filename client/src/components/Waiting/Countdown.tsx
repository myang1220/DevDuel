import { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import BinaryBackground from "../Home/BinaryBackground";

// countdown before game starts
export default function Countdown() {
  const [count, setCount] = useState(3);
  const [message, setMessage] = useState("");
  const location = useLocation();
  const navigate = useNavigate();
  const { roomID } = location.state || {};

  // animation for countdown, decrements every second
  useEffect(() => {
    if (count > 0) {
      const timer = setTimeout(() => {
        setCount(count - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (count === 0) {
      setMessage("GO!");
      const timer = setTimeout(() => {
        setCount(count - 1);
      }, 300);
      return () => clearTimeout(timer);
    } else {
      navigate("/game", { state: { roomID } });
    }
  }, [count, navigate]);

  return (
    <div className="w-screen h-screen">
      <BinaryBackground />
      <div className="w-full h-full flex flex-col justify-center text-7xl font-courier font-bold items-center gap-20 text-gray-400 text-glow">
        <div>{count >= 0 ? `Game Starting In...` : null}</div>
        <div className="text-9xl text-gray-200">
          {count > 0 ? `${count}` : null}
          {count === 0 ? `${message}` : null}
        </div>
      </div>
    </div>
  );
}
