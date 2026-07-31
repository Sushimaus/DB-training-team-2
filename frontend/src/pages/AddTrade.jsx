// TICKET-ADV123 — React Hook Form + Yup validation.
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

// TODO(TICKET-ADV123): build a yup.object schema covering every field on the
//   form. Suggested validators:
//     tradeRef       — string, regex /^[A-Z]{3}-\d{8}-\d{4}$/ ("AAA-YYYYMMDD-NNNN")
//     instrumentId   — integer, positive
//     counterpartyId — integer, positive
//     assetClass     — oneOf ['EQUITY','FX','BOND','DERIVATIVE']
//     side           — oneOf ['BUY','SELL']
//     quantity       — positive number
//     price          — positive number
//     tradeDate      — date
const schema = yup.object({
  tradeRef: yup.string().matches(/^[A-Z]{3}-\d{8}-\d{4}$/, 'Format: AAA-YYYYMMDD-NNNN').required('Trade ref is required'),
  instrumentId: yup
    .number()
    .typeError('Must be a number')
    .integer('Must be a whole number')
    .positive('Must be positive')
    .required('Instrument ID is required'),
  counterpartyId: yup
    .number()
    .typeError('Must be a number')
    .integer('Must be a whole number')
    .positive('Must be positive')
    .required('Counterparty ID is required'),
  assetClass: yup
    .string()
    .oneOf(['EQUITY', 'FX', 'BOND', 'DERIVATIVE'])
    .required('Asset class is required'),
  side: yup
    .string()
    .oneOf(['BUY', 'SELL'])
    .required('Side is required'),
  quantity: yup
    .number()
    .typeError('Must be a number')
    .positive('Must be greater than 0')
    .required('Quantity is required'),
  price: yup
    .number()
    .typeError('Must be a number')
    .positive('Must be greater than 0')
    .required('Price is required'),
  tradeDate: yup
    .date()
    .typeError('Must be a valid date')
    .required('Trade date is required'),
});

function AddTrade() {
  const [serverError, setServerError] = useState(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting, isSubmitSuccessful },
    reset,
  } = useForm({
    resolver: yupResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      tradeRef: '',
      instrumentId: '',
      counterpartyId: '',
      assetClass: 'EQUITY',
      side: 'BUY',
      quantity: '',
      price: '',
      tradeDate: '',
    },
  });

  async function onSubmit(values) {
    setServerError(null);
    try {
      await api.createTrade(values);
      reset();
    } catch (err) {
      setServerError(err.message || 'Failed to create trade');
    }
  }

  return (
    <section>
      <h2>Add trade</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="trade-form" noValidate>
        <label>
          Trade ref
          <input {...register('tradeRef')} placeholder="EQU-20260603-0001" />
        </label>
        {errors.tradeRef && <p role="alert" className="form-error">{errors.tradeRef.message}</p>}

        <label>
          Instrument id
          <input type="number" {...register('instrumentId')} placeholder="1001" />
        </label>
        {errors.instrumentId && <p role="alert" className="form-error">{errors.instrumentId.message}</p>}

        <label>
          Counterparty id
          <input type="number" {...register('counterpartyId')} placeholder="501" />
        </label>
        {errors.counterpartyId && <p role="alert" className="form-error">{errors.counterpartyId.message}</p>}

        <label>
          Asset class
          <select {...register('assetClass')}>
            <option value="EQUITY">EQUITY</option>
            <option value="FX">FX</option>
            <option value="BOND">BOND</option>
            <option value="DERIVATIVE">DERIVATIVE</option>
          </select>
        </label>
        {errors.assetClass && <p role="alert" className="form-error">{errors.assetClass.message}</p>}

        <label>
          Side
          <select {...register('side')}>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </label>
        {errors.side && <p role="alert" className="form-error">{errors.side.message}</p>}

        <label>
          Quantity
          <input type="number" step="0.0001" {...register('quantity')} placeholder="1000" />
        </label>
        {errors.quantity && <p role="alert" className="form-error">{errors.quantity.message}</p>}

        <label>
          Price
          <input type="number" step="0.0001" {...register('price')} placeholder="150.25" />
        </label>
        {errors.price && <p role="alert" className="form-error">{errors.price.message}</p>}

        <label>
          Trade date
          <input type="date" {...register('tradeDate')} />
        </label>
        {errors.tradeDate && <p role="alert" className="form-error">{errors.tradeDate.message}</p>}

        {serverError && <p role="alert" className="form-error">{serverError}</p>}
        {isSubmitSuccessful && !serverError && <p role="status">Trade created.</p>}

        <button disabled={isSubmitting} type="submit">Submit</button>
      </form>
    </section>
  );
}

export default withAuth(AddTrade);