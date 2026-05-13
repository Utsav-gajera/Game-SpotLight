import React from 'react';

export default function Pagination({ currentPage, totalPages, onPageChange, isLoading }) {
  const handlePrevious = () => {
    if (currentPage > 1) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (currentPage < totalPages) {
      onPageChange(currentPage + 1);
    }
  };

  return (
    <div className="mt-8 flex items-center justify-between">
      <button
        type="button"
        onClick={handlePrevious}
        disabled={currentPage === 1 || isLoading}
        className="secondary-button disabled:opacity-50 disabled:cursor-not-allowed"
      >
        ← Previous
      </button>

      <div className="text-center">
        <div className="text-sm text-slate-300">
          Page <span className="font-bold text-white">{currentPage}</span> of <span className="font-bold text-white">{totalPages}</span>
        </div>
      </div>

      <button
        type="button"
        onClick={handleNext}
        disabled={currentPage === totalPages || isLoading}
        className="secondary-button disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Next →
      </button>
    </div>
  );
}
