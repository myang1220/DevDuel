import { useEffect, useState } from "react";

function BinaryBackground() {
  const [positions, setPositions] = useState<number[]>([]);
  const [binaries, setBinaries] = useState<string[][]>([]);
  const speeds = Array.from({ length: 15 }, () => 0.15 + Math.random() * 0.4); // Speed for each stream
  const setCount = 10; // Number of binary streams
  const binaryLength = 34; // Number of characters in each stream
  const lineHeight = 25; // Approximate line height in pixels
  const bottomLimit = window.innerHeight; // Bottom limit of the screen

  // create binary strings, set positions, and set up animation
  useEffect(() => {
    const initialPositions = Array.from({ length: setCount }, (_) => 25);
    const initialBinaries = Array.from({ length: setCount }, () =>
      generateBinaryString(binaryLength)
    );

    setPositions(initialPositions);
    setBinaries(initialBinaries);

    let animationFrame: number;

    const move = () => {
      setPositions((prevPositions) =>
        prevPositions.map((pos, index) => {
          const newPosition = pos + speeds[index];
          return newPosition >= bottomLimit
            ? newPosition - bottomLimit // Wrap seamlessly
            : newPosition;
        })
      );
      animationFrame = requestAnimationFrame(move);
    };

    move();

    return () => cancelAnimationFrame(animationFrame);
  }, []);

  // Generates a random binary string of given length
  function generateBinaryString(length: number) {
    return Array.from({ length }, () => (Math.random() < 0.5 ? "0" : "1"));
  }

  return (
    <div className="absolute inset-0 pointer-events-none">
      {positions.map((position, index) => (
        <div
          key={index}
          className="text-green-500 absolute font-courier"
          style={{
            left: `calc(${(index / setCount) * 100}vw + ${50 / setCount}vw)`,
            opacity: 0.15,
          }}
        >
          {binaries[index].map((bit, i) => {
            // Calculate the position for each character
            const bitPosition = position + i * lineHeight;

            // Wrap characters dynamically
            const wrappedPosition =
              bitPosition >= bottomLimit
                ? bitPosition - bottomLimit
                : bitPosition;

            return (
              <div
                key={i}
                className="inline-block"
                style={{
                  position: "absolute",
                  top: `${wrappedPosition}px`,
                }}
              >
                {bit}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}

export default BinaryBackground;
